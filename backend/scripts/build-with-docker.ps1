param(
    [string]$projectPath = ".",
    [string]$mavenArgs = "-DskipTests package"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
# project root is parent of scripts folder
$projectRoot = Resolve-Path (Join-Path $scriptDir "..")

Write-Host "Building project '$projectPath' using Maven inside Docker..."

try {
    $cwd = $projectRoot.ProviderPath
    docker run --rm -v "${cwd}:/workspace" -w "/workspace/$projectPath" maven:3.8.8-openjdk-17 mvn $mavenArgs
} catch {
    Write-Error "Docker build failed: $_"
    exit 1
}

Write-Host "Docker-based build finished."
