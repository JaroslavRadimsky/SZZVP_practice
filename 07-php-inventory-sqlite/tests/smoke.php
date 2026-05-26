<?php
declare(strict_types=1);

require_once __DIR__ . '/../src/Database.php';
require_once __DIR__ . '/../src/Auth.php';
require_once __DIR__ . '/../src/InventoryRepository.php';

function ensure(bool $condition, string $message): void
{
    if (!$condition) {
        throw new RuntimeException($message);
    }
}

$dir = sys_get_temp_dir() . '/inventory-' . bin2hex(random_bytes(4));
$db = new Database($dir . '/inventory.sqlite3');
$auth = new Auth($db->pdo());
$repo = new InventoryRepository($db->pdo());

$user = $auth->login('admin', 'admin123');
ensure($user !== null && $user['role'] === 'admin', 'Admin login failed.');

$productId = $repo->addProduct([
    'sku' => 'MAT-001',
    'name' => 'Material',
    'description' => 'Test',
    'quantity' => 10,
    'min_quantity' => 2,
    'price' => 25.5,
]);

$orderId = $repo->createOrder('Zakaznik', $productId, 3);
$repo->updateOrderStatus($orderId, 'odeslana');

$products = $repo->products();
$orders = $repo->orders();

ensure(count($products) === 1, 'Product was not created.');
ensure((int)$products[0]['quantity'] === 7, 'Stock was not decremented.');
ensure($orders[0]['status'] === 'odeslana', 'Order status was not updated.');

echo "OK\n";
