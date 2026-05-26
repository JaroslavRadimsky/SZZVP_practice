# Hodnoceni NoSQL reseni

## Vyhody

- Dokument pro jednu zemi a rok obsahuje vsechny potrebne hodnoty pro vetsinu reportu.
- Agregacni pipelines umi prirozene pracovat s vnorenenymi pozorovanimi.
- Schema je pruzne: lze doplnit dalsi pole z nove verze otevrenych dat bez migrace relacnich tabulek.
- Denormalizace zrychluje cteni prehledovych vystupu.

## Nevyhody

- Duplicitni hodnoty, napriklad nazev zeme a HDP, mohou ztizit udrzbu konzistence.
- Pro ad-hoc vztahove dotazy je relacni model prehlednejsi.
- Referencni integrita neni vynucena databazi jako u cizich klicu v PostgreSQL.
- Pri castych zmenach v dimenzich je nutne aktualizovat mnoho dokumentu.

## Zaver

MongoDB je vhodna pro prehledove reporty nad dokumentem zeme-rok. PostgreSQL je vhodnejsi pro formalni datovy sklad, silnou integritu a rozsahle kombinace dimenzi.

