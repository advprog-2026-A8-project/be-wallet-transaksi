param(
    [int]$Users = 3000,
    [int]$Transactions = 50000,
    [string]$DbHost = "localhost",
    [int]$Port = 6003,
    [string]$Database = "walletdb_dev",
    [string]$Username = "wallet",
    [string]$Password = "walletpass"
)

if ($Users -le 0 -or $Transactions -le 0) {
    throw "-Users and -Transactions must be positive values."
}
$resolvedUsers = $Users
$resolvedTransactions = $Transactions

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlFile = Join-Path $scriptDir "seed_wallet.sql"

if (-not (Test-Path $sqlFile)) {
    throw "SQL file not found at: $sqlFile"
}

Write-Host "Seeding to $DbHost`:$Port/$Database (users=$resolvedUsers, txns=$resolvedTransactions)..."

$env:PGPASSWORD = $Password
try {
    psql `
        -h $DbHost `
        -p $Port `
        -U $Username `
        -d $Database `
        -v users=$resolvedUsers `
        -v txns=$resolvedTransactions `
        -f $sqlFile
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}

Write-Host "Seeding finished."
