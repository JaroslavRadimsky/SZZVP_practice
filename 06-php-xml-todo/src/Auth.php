<?php
declare(strict_types=1);

require_once __DIR__ . '/XmlStore.php';

final class Auth
{
    public function __construct(private XmlStore $store)
    {
    }

    public function register(string $username, string $password): string
    {
        $username = trim($username);
        if ($username === '' || strlen($password) < 4) {
            throw new RuntimeException('Zadejte uzivatelske jmeno a heslo alespon 4 znaky.');
        }
        if ($this->findByUsername($username) !== null) {
            throw new RuntimeException('Uzivatel uz existuje.');
        }

        $doc = $this->store->load();
        $user = $doc->createElement('user');
        $id = bin2hex(random_bytes(8));
        $user->setAttribute('id', $id);
        XmlStore::appendText($doc, $user, 'username', $username);
        XmlStore::appendText($doc, $user, 'passwordHash', password_hash($password, PASSWORD_DEFAULT));
        $doc->documentElement->appendChild($user);
        $this->store->save($doc);
        return $id;
    }

    public function login(string $username, string $password): ?array
    {
        $user = $this->findByUsername($username);
        if ($user === null || !password_verify($password, $user['passwordHash'])) {
            return null;
        }
        return $user;
    }

    public function findByUsername(string $username): ?array
    {
        $doc = $this->store->load();
        foreach ($doc->getElementsByTagName('user') as $user) {
            if (XmlStore::text($user, 'username') === $username) {
                return $this->mapUser($user);
            }
        }
        return null;
    }

    private function mapUser(DOMElement $user): array
    {
        return [
            'id' => $user->getAttribute('id'),
            'username' => XmlStore::text($user, 'username'),
            'passwordHash' => XmlStore::text($user, 'passwordHash'),
        ];
    }
}

