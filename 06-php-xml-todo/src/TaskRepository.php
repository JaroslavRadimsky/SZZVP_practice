<?php
declare(strict_types=1);

require_once __DIR__ . '/XmlStore.php';

final class TaskRepository
{
    public const STATUSES = ['nezahajene', 'zahajene', 'dokoncene'];

    public function __construct(private XmlStore $store)
    {
    }

    public function allForUser(string $userId, ?string $category = null, ?string $status = null): array
    {
        $doc = $this->store->load();
        $tasks = [];
        foreach ($doc->getElementsByTagName('task') as $task) {
            $item = $this->mapTask($task);
            if ($item['userId'] !== $userId) {
                continue;
            }
            if ($category && $item['category'] !== $category) {
                continue;
            }
            if ($status && $item['status'] !== $status) {
                continue;
            }
            $tasks[] = $item;
        }
        usort($tasks, fn(array $a, array $b) => strcmp($b['createdAt'], $a['createdAt']));
        return $tasks;
    }

    public function add(string $userId, array $data): string
    {
        $this->validate($data);
        $doc = $this->store->load();
        $task = $doc->createElement('task');
        $id = bin2hex(random_bytes(8));
        $task->setAttribute('id', $id);
        XmlStore::appendText($doc, $task, 'userId', $userId);
        XmlStore::appendText($doc, $task, 'title', trim((string)$data['title']));
        XmlStore::appendText($doc, $task, 'description', trim((string)($data['description'] ?? '')));
        XmlStore::appendText($doc, $task, 'category', trim((string)$data['category']));
        XmlStore::appendText($doc, $task, 'status', trim((string)$data['status']));
        XmlStore::appendText($doc, $task, 'dueDate', trim((string)($data['dueDate'] ?? '')));
        XmlStore::appendText($doc, $task, 'createdAt', gmdate('c'));
        $doc->documentElement->appendChild($task);
        $this->store->save($doc);
        return $id;
    }

    public function update(string $userId, string $id, array $data): void
    {
        $this->validate($data);
        $doc = $this->store->load();
        $task = $this->findElement($doc, $userId, $id);
        if ($task === null) {
            throw new RuntimeException('Ukol nebyl nalezen.');
        }
        foreach (['title', 'description', 'category', 'status', 'dueDate'] as $field) {
            $nodes = $task->getElementsByTagName($field);
            if ($nodes->length > 0) {
                $nodes->item(0)->nodeValue = trim((string)($data[$field] ?? ''));
            }
        }
        $this->store->save($doc);
    }

    public function delete(string $userId, string $id): void
    {
        $doc = $this->store->load();
        $task = $this->findElement($doc, $userId, $id);
        if ($task !== null) {
            $task->parentNode?->removeChild($task);
            $this->store->save($doc);
        }
    }

    public function importXml(string $userId, string $uploadedPath, string $xsdPath): string
    {
        $doc = new DOMDocument('1.0', 'UTF-8');
        $doc->load($uploadedPath);
        if (!$doc->schemaValidate($xsdPath)) {
            throw new RuntimeException('Importovany XML soubor neodpovida XSD.');
        }
        $root = $doc->documentElement;
        return $this->add($userId, [
            'title' => XmlStore::text($root, 'title'),
            'description' => XmlStore::text($root, 'description'),
            'category' => XmlStore::text($root, 'category'),
            'status' => XmlStore::text($root, 'status'),
            'dueDate' => XmlStore::text($root, 'dueDate'),
        ]);
    }

    private function findElement(DOMDocument $doc, string $userId, string $id): ?DOMElement
    {
        foreach ($doc->getElementsByTagName('task') as $task) {
            if ($task->getAttribute('id') === $id && XmlStore::text($task, 'userId') === $userId) {
                return $task;
            }
        }
        return null;
    }

    private function mapTask(DOMElement $task): array
    {
        return [
            'id' => $task->getAttribute('id'),
            'userId' => XmlStore::text($task, 'userId'),
            'title' => XmlStore::text($task, 'title'),
            'description' => XmlStore::text($task, 'description'),
            'category' => XmlStore::text($task, 'category'),
            'status' => XmlStore::text($task, 'status'),
            'dueDate' => XmlStore::text($task, 'dueDate'),
            'createdAt' => XmlStore::text($task, 'createdAt'),
        ];
    }

    private function validate(array $data): void
    {
        if (trim((string)($data['title'] ?? '')) === '') {
            throw new RuntimeException('Nazev ukolu je povinny.');
        }
        if (trim((string)($data['category'] ?? '')) === '') {
            throw new RuntimeException('Kategorie je povinna.');
        }
        if (!in_array((string)($data['status'] ?? ''), self::STATUSES, true)) {
            throw new RuntimeException('Neplatny stav ukolu.');
        }
    }
}

