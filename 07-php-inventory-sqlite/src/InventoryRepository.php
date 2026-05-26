<?php
declare(strict_types=1);

final class InventoryRepository
{
    public function __construct(private PDO $pdo)
    {
    }

    public function products(): array
    {
        return $this->pdo->query('SELECT *, quantity <= min_quantity AS low_stock FROM products ORDER BY name')->fetchAll();
    }

    public function addProduct(array $data): int
    {
        $stmt = $this->pdo->prepare(
            'INSERT INTO products(sku, name, description, quantity, min_quantity, price) VALUES (?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            trim((string)$data['sku']),
            trim((string)$data['name']),
            trim((string)($data['description'] ?? '')),
            (int)($data['quantity'] ?? 0),
            (int)($data['min_quantity'] ?? 0),
            (float)($data['price'] ?? 0),
        ]);
        return (int)$this->pdo->lastInsertId();
    }

    public function updateProduct(int $id, array $data): void
    {
        $stmt = $this->pdo->prepare(
            'UPDATE products SET sku = ?, name = ?, description = ?, quantity = ?, min_quantity = ?, price = ? WHERE id = ?'
        );
        $stmt->execute([
            trim((string)$data['sku']),
            trim((string)$data['name']),
            trim((string)($data['description'] ?? '')),
            (int)($data['quantity'] ?? 0),
            (int)($data['min_quantity'] ?? 0),
            (float)($data['price'] ?? 0),
            $id,
        ]);
    }

    public function deleteProduct(int $id): void
    {
        $stmt = $this->pdo->prepare('DELETE FROM products WHERE id = ?');
        $stmt->execute([$id]);
    }

    public function createOrder(string $customerName, int $productId, int $quantity): int
    {
        if ($quantity <= 0) {
            throw new RuntimeException('Mnozstvi musi byt kladne.');
        }
        $this->pdo->beginTransaction();
        try {
            $stock = $this->pdo->prepare('SELECT quantity FROM products WHERE id = ?');
            $stock->execute([$productId]);
            $available = $stock->fetchColumn();
            if ($available === false || (int)$available < $quantity) {
                throw new RuntimeException('Nedostatek zasob.');
            }
            $order = $this->pdo->prepare('INSERT INTO orders(customer_name, status, created_at) VALUES (?, ?, ?)');
            $order->execute([trim($customerName), 'nova', gmdate('c')]);
            $orderId = (int)$this->pdo->lastInsertId();

            $item = $this->pdo->prepare('INSERT INTO order_items(order_id, product_id, quantity) VALUES (?, ?, ?)');
            $item->execute([$orderId, $productId, $quantity]);

            $update = $this->pdo->prepare('UPDATE products SET quantity = quantity - ? WHERE id = ?');
            $update->execute([$quantity, $productId]);
            $this->pdo->commit();
            return $orderId;
        } catch (Throwable $e) {
            $this->pdo->rollBack();
            throw $e;
        }
    }

    public function orders(): array
    {
        return $this->pdo->query(
            <<<SQL
            SELECT o.id, o.customer_name, o.status, o.created_at,
                   p.name AS product_name, oi.quantity
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            JOIN products p ON p.id = oi.product_id
            ORDER BY o.created_at DESC, o.id DESC
            SQL
        )->fetchAll();
    }

    public function updateOrderStatus(int $id, string $status): void
    {
        $allowed = ['nova', 'pripravuje_se', 'odeslana', 'zrusena'];
        if (!in_array($status, $allowed, true)) {
            throw new RuntimeException('Neplatny stav objednavky.');
        }
        $stmt = $this->pdo->prepare('UPDATE orders SET status = ? WHERE id = ?');
        $stmt->execute([$status, $id]);
    }
}
