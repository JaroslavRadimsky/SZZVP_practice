<?php
declare(strict_types=1);

require_once __DIR__ . '/../src/bootstrap.php';

$message = '';
try {
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $action = $_POST['action'] ?? '';
        if ($action === 'login') {
            $user = $auth->login($_POST['username'] ?? '', $_POST['password'] ?? '');
            if (!$user) {
                throw new RuntimeException('Neplatne prihlaseni.');
            }
            $_SESSION['user'] = $user;
        } elseif ($action === 'logout') {
            session_destroy();
            header('Location: /');
            exit;
        } else {
            require_login();
            if ($action === 'add_product') {
                require_admin();
                $inventory->addProduct($_POST);
            } elseif ($action === 'update_product') {
                require_admin();
                $inventory->updateProduct((int)$_POST['id'], $_POST);
            } elseif ($action === 'delete_product') {
                require_admin();
                $inventory->deleteProduct((int)$_POST['id']);
            } elseif ($action === 'create_order') {
                $inventory->createOrder($_POST['customer_name'] ?? '', (int)$_POST['product_id'], (int)$_POST['quantity']);
            } elseif ($action === 'update_order') {
                $inventory->updateOrderStatus((int)$_POST['id'], $_POST['status'] ?? 'nova');
            }
        }
    }
} catch (Throwable $e) {
    $message = $e->getMessage();
}

$user = current_user();
$products = $user ? $inventory->products() : [];
$orders = $user ? $inventory->orders() : [];
?>
<!doctype html>
<html lang="cs">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Skladovy system</title>
    <link rel="stylesheet" href="/styles.css">
</head>
<body>
<main>
    <header>
        <h1>Skladovy system</h1>
        <?php if ($user): ?>
            <form method="post">
                <span><?= h($user['username']) ?> (<?= h($user['role']) ?>)</span>
                <button name="action" value="logout">Odhlasit</button>
            </form>
        <?php endif; ?>
    </header>

    <?php if ($message): ?><p class="message"><?= h($message) ?></p><?php endif; ?>

    <?php if (!$user): ?>
        <form method="post" class="panel login">
            <h2>Prihlaseni</h2>
            <input name="username" placeholder="Uzivatelske jmeno" required>
            <input name="password" type="password" placeholder="Heslo" required>
            <button name="action" value="login">Prihlasit</button>
        </form>
    <?php else: ?>
        <?php if ($user['role'] === 'admin'): ?>
            <section class="panel">
                <h2>Novy produkt</h2>
                <form method="post" class="row">
                    <input name="sku" placeholder="SKU" required>
                    <input name="name" placeholder="Nazev" required>
                    <input name="description" placeholder="Popis">
                    <input name="quantity" type="number" placeholder="Mnozstvi" required>
                    <input name="min_quantity" type="number" placeholder="Minimum" required>
                    <input name="price" type="number" step="0.01" placeholder="Cena" required>
                    <button name="action" value="add_product">Pridat</button>
                </form>
            </section>
        <?php endif; ?>

        <section class="panel">
            <h2>Produkty</h2>
            <div class="table">
                <?php foreach ($products as $product): ?>
                    <form method="post" class="row <?= (int)$product['low_stock'] === 1 ? 'low' : '' ?>">
                        <input type="hidden" name="id" value="<?= (int)$product['id'] ?>">
                        <input name="sku" value="<?= h($product['sku']) ?>" <?= $user['role'] !== 'admin' ? 'readonly' : '' ?>>
                        <input name="name" value="<?= h($product['name']) ?>" <?= $user['role'] !== 'admin' ? 'readonly' : '' ?>>
                        <input name="description" value="<?= h($product['description']) ?>" <?= $user['role'] !== 'admin' ? 'readonly' : '' ?>>
                        <input name="quantity" type="number" value="<?= (int)$product['quantity'] ?>" <?= $user['role'] !== 'admin' ? 'readonly' : '' ?>>
                        <input name="min_quantity" type="number" value="<?= (int)$product['min_quantity'] ?>" <?= $user['role'] !== 'admin' ? 'readonly' : '' ?>>
                        <input name="price" type="number" step="0.01" value="<?= h((string)$product['price']) ?>" <?= $user['role'] !== 'admin' ? 'readonly' : '' ?>>
                        <?php if ($user['role'] === 'admin'): ?>
                            <button name="action" value="update_product">Ulozit</button>
                            <button name="action" value="delete_product">Smazat</button>
                        <?php endif; ?>
                    </form>
                <?php endforeach; ?>
            </div>
        </section>

        <section class="panel">
            <h2>Nova objednavka</h2>
            <form method="post" class="row order">
                <input name="customer_name" placeholder="Zakaznik" required>
                <select name="product_id">
                    <?php foreach ($products as $product): ?>
                        <option value="<?= (int)$product['id'] ?>"><?= h($product['name']) ?> (<?= (int)$product['quantity'] ?> ks)</option>
                    <?php endforeach; ?>
                </select>
                <input name="quantity" type="number" value="1" min="1">
                <button name="action" value="create_order">Vytvorit</button>
            </form>
        </section>

        <section class="panel">
            <h2>Objednavky</h2>
            <?php foreach ($orders as $order): ?>
                <form method="post" class="row order-line">
                    <input type="hidden" name="id" value="<?= (int)$order['id'] ?>">
                    <span>#<?= (int)$order['id'] ?> <?= h($order['customer_name']) ?>: <?= h($order['product_name']) ?> x <?= (int)$order['quantity'] ?></span>
                    <select name="status">
                        <?php foreach (['nova', 'pripravuje_se', 'odeslana', 'zrusena'] as $status): ?>
                            <option value="<?= h($status) ?>" <?= $order['status'] === $status ? 'selected' : '' ?>><?= h($status) ?></option>
                        <?php endforeach; ?>
                    </select>
                    <button name="action" value="update_order">Zmenit stav</button>
                </form>
            <?php endforeach; ?>
        </section>
    <?php endif; ?>
</main>
</body>
</html>

