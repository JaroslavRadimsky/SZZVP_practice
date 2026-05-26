<?php
declare(strict_types=1);

session_start();

require_once __DIR__ . '/Database.php';
require_once __DIR__ . '/Auth.php';
require_once __DIR__ . '/InventoryRepository.php';

$dbPath = getenv('INVENTORY_DB') ?: __DIR__ . '/../storage/inventory.sqlite3';
$database = new Database($dbPath);
$pdo = $database->pdo();
$auth = new Auth($pdo);
$inventory = new InventoryRepository($pdo);

function h(mixed $value): string
{
    return htmlspecialchars((string)($value ?? ''), ENT_QUOTES, 'UTF-8');
}

function current_user(): ?array
{
    $user = $_SESSION['user'] ?? null;
    if (!is_array($user) || !isset($user['username'], $user['role'])) {
        unset($_SESSION['user']);
        return null;
    }
    return $user;
}

function require_login(): void
{
    if (!current_user()) {
        header('Location: /');
        exit;
    }
}

function require_admin(): void
{
    if ((current_user()['role'] ?? '') !== 'admin') {
        throw new RuntimeException('Akce vyzaduje roli admin.');
    }
}

function require_api_key(): void
{
    if (($_SERVER['HTTP_X_API_KEY'] ?? '') !== 'demo-key') {
        http_response_code(401);
        header('Content-Type: application/json');
        echo json_encode(['error' => 'Unauthorized']);
        exit;
    }
}
