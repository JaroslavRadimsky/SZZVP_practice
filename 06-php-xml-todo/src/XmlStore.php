<?php
declare(strict_types=1);

final class XmlStore
{
    public function __construct(private string $path, private string $rootName)
    {
        if (!file_exists($this->path)) {
            $doc = new DOMDocument('1.0', 'UTF-8');
            $doc->appendChild($doc->createElement($this->rootName));
            $doc->formatOutput = true;
            $doc->save($this->path);
        }
    }

    public function load(): DOMDocument
    {
        $doc = new DOMDocument('1.0', 'UTF-8');
        $doc->preserveWhiteSpace = false;
        $doc->formatOutput = true;
        $doc->load($this->path);
        return $doc;
    }

    public function save(DOMDocument $doc): void
    {
        $doc->formatOutput = true;
        $doc->save($this->path);
    }

    public static function text(DOMElement $parent, string $name): string
    {
        $nodes = $parent->getElementsByTagName($name);
        return $nodes->length > 0 ? trim($nodes->item(0)->textContent ?? '') : '';
    }

    public static function appendText(DOMDocument $doc, DOMElement $parent, string $name, string $value): void
    {
        $element = $doc->createElement($name);
        $element->appendChild($doc->createTextNode($value));
        $parent->appendChild($element);
    }
}

