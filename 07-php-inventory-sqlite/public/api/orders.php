<?php
declare(strict_types=1);

require_once __DIR__ . '/../../src/bootstrap.php';
require_api_key();
header('Content-Type: application/json');

try {
    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        echo json_encode($inventory->orders());
        exit;
    }

    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $data = json_decode(file_get_contents('php://input'), true, flags: JSON_THROW_ON_ERROR);
        $id = $inventory->createOrder(
            (string)($data['customer_name'] ?? ''),
            (int)($data['product_id'] ?? 0),
            (int)($data['quantity'] ?? 0)
        );
        http_response_code(201);
        echo json_encode(['id' => $id]);
        exit;
    }

    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
} catch (Throwable $e) {
    http_response_code(400);
    echo json_encode(['error' => $e->getMessage()]);
}

