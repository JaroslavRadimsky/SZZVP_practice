<?php
declare(strict_types=1);

session_start();

require_once __DIR__ . '/../src/Auth.php';
require_once __DIR__ . '/../src/TaskRepository.php';

$auth = new Auth(new XmlStore(__DIR__ . '/../storage/users.xml', 'users'));
$tasks = new TaskRepository(new XmlStore(__DIR__ . '/../storage/tasks.xml', 'tasks'));
$message = '';

function h(string $value): string
{
    return htmlspecialchars($value, ENT_QUOTES, 'UTF-8');
}

try {
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $action = $_POST['action'] ?? '';
        if ($action === 'register') {
            $id = $auth->register($_POST['username'] ?? '', $_POST['password'] ?? '');
            $_SESSION['user'] = ['id' => $id, 'username' => $_POST['username'] ?? ''];
        } elseif ($action === 'login') {
            $user = $auth->login($_POST['username'] ?? '', $_POST['password'] ?? '');
            if ($user === null) {
                throw new RuntimeException('Neplatne prihlaseni.');
            }
            $_SESSION['user'] = ['id' => $user['id'], 'username' => $user['username']];
        } elseif ($action === 'logout') {
            session_destroy();
            header('Location: /');
            exit;
        } elseif (isset($_SESSION['user'])) {
            $userId = $_SESSION['user']['id'];
            if ($action === 'add') {
                $tasks->add($userId, $_POST);
            } elseif ($action === 'update') {
                $tasks->update($userId, $_POST['id'] ?? '', $_POST);
            } elseif ($action === 'delete') {
                $tasks->delete($userId, $_POST['id'] ?? '');
            } elseif ($action === 'import' && isset($_FILES['import_xml'])) {
                $tasks->importXml($userId, $_FILES['import_xml']['tmp_name'], __DIR__ . '/../schemas/task.xsd');
            }
        }
    }
} catch (Throwable $e) {
    $message = $e->getMessage();
}

$user = $_SESSION['user'] ?? null;
$category = $_GET['category'] ?? '';
$status = $_GET['status'] ?? '';
$visibleTasks = $user ? $tasks->allForUser($user['id'], $category ?: null, $status ?: null) : [];
?>
<!doctype html>
<html lang="cs">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>XML zapisnik ukolu</title>
    <link rel="stylesheet" href="/styles.css">
</head>
<body>
<main>
    <header>
        <h1>XML zapisnik ukolu</h1>
        <?php if ($user): ?>
            <form method="post">
                <span>Prihlasen: <?= h($user['username']) ?></span>
                <button name="action" value="logout">Odhlasit</button>
            </form>
        <?php endif; ?>
    </header>

    <?php if ($message): ?><p class="message"><?= h($message) ?></p><?php endif; ?>

    <?php if (!$user): ?>
        <section class="grid two">
            <form method="post" class="panel">
                <h2>Registrace</h2>
                <input name="username" placeholder="Uzivatelske jmeno" required>
                <input name="password" placeholder="Heslo" type="password" required>
                <button name="action" value="register">Registrovat</button>
            </form>
            <form method="post" class="panel">
                <h2>Prihlaseni</h2>
                <input name="username" placeholder="Uzivatelske jmeno" required>
                <input name="password" placeholder="Heslo" type="password" required>
                <button name="action" value="login">Prihlasit</button>
            </form>
        </section>
    <?php else: ?>
        <section class="grid two">
            <form method="post" class="panel">
                <h2>Novy ukol</h2>
                <input name="title" placeholder="Nazev" required>
                <textarea name="description" placeholder="Popis"></textarea>
                <input name="category" placeholder="Kategorie" value="studium" required>
                <select name="status">
                    <?php foreach (TaskRepository::STATUSES as $item): ?>
                        <option value="<?= h($item) ?>"><?= h($item) ?></option>
                    <?php endforeach; ?>
                </select>
                <input name="dueDate" placeholder="Termin">
                <button name="action" value="add">Pridat</button>
            </form>

            <form method="post" enctype="multipart/form-data" class="panel">
                <h2>Import XML</h2>
                <input type="file" name="import_xml" accept=".xml" required>
                <button name="action" value="import">Importovat</button>
            </form>
        </section>

        <form class="filters" method="get">
            <input name="category" placeholder="Filtr kategorie" value="<?= h($category) ?>">
            <select name="status">
                <option value="">Vsechny stavy</option>
                <?php foreach (TaskRepository::STATUSES as $item): ?>
                    <option value="<?= h($item) ?>" <?= $status === $item ? 'selected' : '' ?>><?= h($item) ?></option>
                <?php endforeach; ?>
            </select>
            <button>Filtrovat</button>
        </form>

        <section class="tasks">
            <?php foreach ($visibleTasks as $task): ?>
                <form method="post" class="task">
                    <input type="hidden" name="id" value="<?= h($task['id']) ?>">
                    <input name="title" value="<?= h($task['title']) ?>" required>
                    <textarea name="description"><?= h($task['description']) ?></textarea>
                    <input name="category" value="<?= h($task['category']) ?>" required>
                    <select name="status">
                        <?php foreach (TaskRepository::STATUSES as $item): ?>
                            <option value="<?= h($item) ?>" <?= $task['status'] === $item ? 'selected' : '' ?>><?= h($item) ?></option>
                        <?php endforeach; ?>
                    </select>
                    <input name="dueDate" value="<?= h($task['dueDate']) ?>" placeholder="Termin">
                    <div class="actions">
                        <button name="action" value="update">Ulozit</button>
                        <button name="action" value="delete">Smazat</button>
                    </div>
                </form>
            <?php endforeach; ?>
        </section>
    <?php endif; ?>
</main>
</body>
</html>

