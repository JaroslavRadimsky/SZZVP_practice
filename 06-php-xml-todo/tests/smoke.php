<?php
declare(strict_types=1);

require_once __DIR__ . '/../src/Auth.php';
require_once __DIR__ . '/../src/TaskRepository.php';

function ensure(bool $condition, string $message): void
{
    if (!$condition) {
        throw new RuntimeException($message);
    }
}

$dir = sys_get_temp_dir() . '/xml-todo-' . bin2hex(random_bytes(4));
mkdir($dir);

$auth = new Auth(new XmlStore($dir . '/users.xml', 'users'));
$repo = new TaskRepository(new XmlStore($dir . '/tasks.xml', 'tasks'));

$userId = $auth->register('tester', 'secret');
$user = $auth->login('tester', 'secret');
ensure($user !== null, 'Login failed.');

$taskId = $repo->add($userId, [
    'title' => 'Dokoncit test',
    'description' => 'Smoke test',
    'category' => 'studium',
    'status' => 'nezahajene',
    'dueDate' => '2026-05-26',
]);
ensure(count($repo->allForUser($userId)) === 1, 'Task was not created.');

$repo->update($userId, $taskId, [
    'title' => 'Dokoncit test',
    'description' => 'Hotovo',
    'category' => 'studium',
    'status' => 'dokoncene',
    'dueDate' => '2026-05-26',
]);
ensure($repo->allForUser($userId, null, 'dokoncene')[0]['description'] === 'Hotovo', 'Task was not updated.');

$importPath = $dir . '/import.xml';
file_put_contents($importPath, '<task><title>Import</title><description>XML</description><category>prace</category><status>zahajene</status><dueDate>2026-05-27</dueDate></task>');
$repo->importXml($userId, $importPath, __DIR__ . '/../schemas/task.xsd');
ensure(count($repo->allForUser($userId)) === 2, 'Task import failed.');

echo "OK\n";
