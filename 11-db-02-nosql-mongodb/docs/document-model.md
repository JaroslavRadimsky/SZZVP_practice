# Dokumentovy model

Kolekce: `country_year_foreigners`

```json
{
  "_id": "UKR-2024",
  "country": {
    "csu_name": "Ukrajina",
    "iso3": "UKR",
    "world_bank_name": "Ukraine"
  },
  "year": 2024,
  "gdp_per_capita_usd": 5342.1,
  "gdp_bucket": "nizke HDP",
  "observations": [
    {
      "sex_code": "TOTAL",
      "sex": "Celkem",
      "age_group": "Celkem",
      "region": "Česko",
      "residence_type": "Celkem",
      "count": 360000
    }
  ]
}
```

Jednotkou dokumentu je statni obcanstvi a rok. Pozorovani podle pohlavi a dalsich dimenzi jsou vnorena, proto typicke vystupy nepotrebuji `JOIN`.

