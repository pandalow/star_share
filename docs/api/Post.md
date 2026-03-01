## POST API

- Description: 

### 1. CreateDraft

Description: Creates a new post draft.

**EndPoint:**
```
POST api/v1/posts/draft
```

**Authentication:** 
Required (Bearer Token in `Authorization` header)

**Request Body:**
None
**Response:**

- **Success (201 Created):**
```json
{
    "id":"01",
}
```

**Example Request:**
```bash
curl -X POST https://api.example.com/api/posts/draft \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 2. Confirm Content

Description: Confirms draft content after upload. Will get the content from OSS services;

**EndPoint:**
```
POST api/v1/posts//{id}/content/confirm
```

**Authentication:** 
Required (Bearer Token in `Authorization` header)

**Request Body:**
```json
{
  "objectKey": "uploads/2026/02/abc123.txt",
  "etag": "d41d8cd98f00b204e9800998ecf8427e",
  "size": 2048,
  "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
}
```

**Response:**

- **Success (201 Created):**
```json
{
    "success": true
}
```

**Example Request:**
```bash
curl -X POST https://api.example.com/api/posts/1234567890/confirm \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "objectKey": "uploads/2026/02/abc123.txt",
    "etag": "d41d8cd98f00b204e9800998ecf8427e",
    "size": 2048,
    "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
  }'
```


### 3. Update Meta Data
Description: Updates post metadata (title, tags, visibility, etc)

**EndPoint:**
```
UPDATE api/v1/posts/{id}
```
**Authentication:** 
Required (Bearer Token in `Authorization` header)

**Request Body:**
```json
{
    "title":"Cool",
    "tagId": 2,
    "tags": ["social", "economics"],
    "imgUrls":["http://dddd.com", "http://ddadfa.com"],
    "visible": "",
    "isTop": true,
    "description": "Some describe"
}
```

**Response:**
- **Success (201 Created):**
```json
{
    "success": true
}
```

**Example Request:**
```bash
curl -X UPDATE https://api.example.com/api/posts/1234567890 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Cool",
    "tagId": 2,
    "tags": ["social", "economics"],
    "imgUrls":["http://dddd.com", "http://ddadfa.com"],
    "visible": "",
    "isTop": true,
    "description": "Some describe"
    }'
```
### 3. Publish Post
Description: Publish the Post, make it visible to all users.

**EndPoint:**
```
UPDATE api/v1/posts/{id}/publish
```
**Authentication:** 
Required (Bearer Token in `Authorization` header)

**Request Body:**
NONE

**Response:**
- **Success (201 Created):**
```json
{
    "success": true
}
```
**Example Request:**
```bash
curl -X UPDATE https://api.example.com/api/posts/1234567890/publish \
  -H "Authorization: Bearer YOUR_TOKEN" 
```

### 3. Update Meta Data
Description: Updates post metadata (title, tags, visibility, etc)

**EndPoint:**
**Authentication:** 
**Request Body:**
**Response:**