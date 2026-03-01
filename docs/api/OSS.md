## OSS API

- Description: OSSController endpoint for generating pre-signed upload URLs.

### 1. Presign
Description: Creates a temporary PUT URL for uploading post content or images.

**EndPoint:**
```
POST api/v1/storage/presign
```

**Authentication:**
Required

**Request Body:**
```json
{
  "scene": "knowpost_content",
  "postId": "1234567890",
  "contentType": "text/markdown",
  "ext": ".md"
}
```

**Response:**
- **Success (200 OK):**
```json
{
  "objectKey": "posts/1234567890/content.md",
  "putUrl": "https://oss.example.com/presign?...",
  "headers": {
    "Content-Type": "text/markdown"
  },
  "expireIn": 600
}
```

**Example Request:**
```bash
curl -X POST https://api.example.com/api/v1/storage/presign \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"scene":"knowpost_image","postId":"123","contentType":"image/png"}'
```
