<#
.SYNOPSIS
    Runs the M2a realism search on the performance cores only, at below-normal priority.

.DESCRIPTION
    Finds the P-core logical processors from Windows CPU set information (the CPU sets with the
    highest efficiency class: the P-cores on a hybrid Intel CPU, every processor on other CPUs),
    builds the search's classpath with Gradle, starts the search JVM with its processor affinity
    held to those processors and below-normal priority, and waits for it. Workers default to the
    P-core thread count minus two (14 on the owner's i9-12900K), which leaves one P-core and all
    E-cores free. The report and genomes go to -Out (build/search by default).

.EXAMPLE
    .\scripts\search.ps1
.EXAMPLE
    .\scripts\search.ps1 -DryRun
.EXAMPLE
    .\scripts\search.ps1 -Generations 1 -Population 2 -Keep 1 -Minutes 1 -Full 0 -Out build/search-check
#>
param(
    [int]$Generations = 8,
    [int]$Population = 40,
    [int]$Keep = 8,
    [int]$Minutes = 20,
    [int]$Full = 5,
    [double]$FullScale = 1.0,
    [int]$Workers = 0,
    [string]$Out = "build/search",
    [string]$JavaHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.10.7-hotspot",
    [string]$Heap = "6g",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

if (-not ("AntFarm.CpuSets" -as [type])) {
    Add-Type -TypeDefinition @"
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Runtime.InteropServices;

namespace AntFarm {
    public static class CpuSets {
        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern bool GetSystemCpuSetInformation(
            IntPtr information, uint bufferLength, out uint returnedLength, IntPtr process, uint flags);

        // One entry per logical processor in processor group 0: (logical index << 8) | efficiency class.
        // SYSTEM_CPU_SET_INFORMATION: Size (4 bytes) at 0, Type (4) at 4; for Type 0 (CpuSetInformation):
        // Id (4) at 8, Group (2) at 12, LogicalProcessorIndex (1) at 14, CoreIndex (1) at 15,
        // LastLevelCacheIndex (1) at 16, NumaNodeIndex (1) at 17, EfficiencyClass (1) at 18.
        public static int[] Read() {
            uint length;
            GetSystemCpuSetInformation(IntPtr.Zero, 0, out length, IntPtr.Zero, 0);
            if (length == 0) throw new Win32Exception(Marshal.GetLastWin32Error());
            IntPtr buffer = Marshal.AllocHGlobal((int)length);
            try {
                if (!GetSystemCpuSetInformation(buffer, length, out length, IntPtr.Zero, 0))
                    throw new Win32Exception(Marshal.GetLastWin32Error());
                var result = new List<int>();
                int offset = 0;
                while (offset < length) {
                    int size = Marshal.ReadInt32(buffer, offset);
                    int type = Marshal.ReadInt32(buffer, offset + 4);
                    if (type == 0) {
                        int group = Marshal.ReadInt16(buffer, offset + 12);
                        int logical = Marshal.ReadByte(buffer, offset + 14);
                        int efficiency = Marshal.ReadByte(buffer, offset + 18);
                        if (group == 0) result.Add((logical << 8) | efficiency);
                    }
                    if (size <= 0) break;
                    offset += size;
                }
                return result.ToArray();
            } finally {
                Marshal.FreeHGlobal(buffer);
            }
        }
    }
}
"@
}

$sets = [AntFarm.CpuSets]::Read()
$top = ($sets | ForEach-Object { $_ -band 0xFF } | Measure-Object -Maximum).Maximum
$pThreads = @($sets | Where-Object { ($_ -band 0xFF) -eq $top } | ForEach-Object { $_ -shr 8 } | Sort-Object)
[long]$mask = 0
foreach ($i in $pThreads) { $mask = $mask -bor ([long]1 -shl $i) }
if ($Workers -le 0) { $Workers = [Math]::Max(1, $pThreads.Count - 2) }

Write-Host ("P-core threads (efficiency class {0}): {1}" -f $top, ($pThreads -join ","))
Write-Host ("Affinity mask 0x{0:X}, {1} workers" -f $mask, $Workers)
if ($DryRun) { return }

$env:JAVA_HOME = $JavaHome
& (Join-Path $root "gradlew.bat") -q -p $root :sim:searchClasspath
if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
$cp = (Get-Content (Join-Path $root "sim\build\search-classpath.txt") -Raw).Trim()

$java = Join-Path $JavaHome "bin\java.exe"
$invariant = [System.Globalization.CultureInfo]::InvariantCulture
$javaArgs = @(
    "-Xmx$Heap", "-cp", "`"$cp`"", "com.bydesigninteractive.ant.sim.search.SearchMainKt",
    "--generations", $Generations, "--population", $Population, "--keep", $Keep,
    "--minutes", $Minutes, "--full", $Full, "--full-scale", $FullScale.ToString($invariant),
    "--workers", $Workers, "--out", $Out
)
$p = Start-Process -FilePath $java -ArgumentList $javaArgs -WorkingDirectory $root -NoNewWindow -PassThru
$null = $p.Handle # hold the handle so the exit code can be read after the exit
$p.ProcessorAffinity = [IntPtr]$mask
$p.PriorityClass = [System.Diagnostics.ProcessPriorityClass]::BelowNormal
Write-Host ("Search JVM {0}: affinity 0x{1:X}, priority {2}" -f $p.Id, $p.ProcessorAffinity.ToInt64(), $p.PriorityClass)
$p.WaitForExit()
exit $p.ExitCode
