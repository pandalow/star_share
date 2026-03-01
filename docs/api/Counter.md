## COUNTER API

- Description: CounterController endpoint for retrieving entity counters.

### 1. GetCounts
Description: Returns requested metrics (like/fav) for a specific entity type/id.

**EndPoint:**
```
GET api/v1/counter/{entityType}/{entityId}
```

**Authentication:**
Not required

**Request Body:**
None (use `metrics` query parameter such as `metrics=like,fav`)

**Response:**
- **Success (200 OK):**
```json
{
  "entityType": "post",
  "entityId": "1234567890",
  "counts": {
    "like": 120,
    "fav": 15
  }
}
```

**Example Request:**
```bash
curl -X GET 'https://api.example.com/api/v1/counter/post/1234567890?metrics=like,fav'
```
