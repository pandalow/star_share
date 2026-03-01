## POST API

- Description: PostController endpoints for drafts, publishing, feeds, and metadata updates.

### 1. Create Draft
Description: Creates a new empty draft for the authenticated user.

**EndPoint:**
```
POST api/v1/posts/draft
```

**Authentication:**
Required (Bearer Token in `Authorization` header)

**Request Body:**
None

**Response:**
- **Success (200 OK):**
```json
{
  "id": "01"
}
```

**Example Request:**
```bash
curl -X POST https://api.example.com/api/v1/posts/draft \
  -H "Authorization: Bearer <TOKEN>"
```

### 2. Confirm Content
Description: Confirms draft content after uploading files to OSS.

**EndPoint:**
```
POST api/v1/posts/{id}/content/confirm
```

**Authentication:**
Required

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
- **Success (200 OK):**
```json
{
  "success": true
}
```

**Example Request:**
```bash
curl -X POST https://api.example.com/api/v1/posts/123/content/confirm \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "objectKey": "uploads/2026/02/abc123.txt",
    "etag": "d41d8cd98f00b204e9800998ecf8427e",
    "size": 2048,
    "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
  }'
```

### 3. Update Meta Data
Description: Partially updates title, tags, cover images, visibility, pin flag, or description.

**EndPoint:**
```
PATCH api/v1/posts/{id}
```
**Authentication:**
Required

**Request Body:**
```json
{
  "title": "Cool",
  "tagId": 2,
  "tags": ["social", "economics"],
  "imgUrls": ["http://dddd.com"],
  "visible": "public",
  "isTop": true,
  "description": "Some describe"
}
```

**Response:**
- **Success (200 OK):**
```json
{
  "success": true
}
```

**Example Request:**
```bash
curl -X PATCH https://api.example.com/api/v1/posts/123 \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Cool"}'
```

### 4. Publish Post
Description: Publishes a draft.

**EndPoint:**
```
PATCH api/v1/posts/{id}/publish
```

**Authentication:**
Required

**Request Body:**
None

**Response:**
- **Success (200 OK):**
```json
{
  "success": true
}
```

**Example Request:**
```bash
curl -X PATCH https://api.example.com/api/v1/posts/123/publish \
  -H "Authorization: Bearer <TOKEN>"
```

### 5. Pin Post
Description: Sets or unsets the `isTop` flag.

**EndPoint:**
```
PATCH api/v1/posts/{id}/top
```

**Authentication:**
Required

**Request Body:**
```json
{
  "isTop": true
}
```

**Response:**
- **Success (200 OK):**
```json
{
  "success": true
}
```

**Example Request:**
```bash
curl -X PATCH https://api.example.com/api/v1/posts/123/top \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"isTop": false}'
```

### 6. Change Visibility
Description: Updates who can view the post (e.g., public/followers/private).

**EndPoint:**
```
PATCH api/v1/posts/{id}/visibility
```

**Authentication:**
Required

**Request Body:**
```json
{
  "visible": "followers"
}
```

**Response:**
- **Success (200 OK):**
```json
{
  "success": true
}
```

**Example Request:**
```bash
curl -X PATCH https://api.example.com/api/v1/posts/123/visibility \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"visible":"private"}'
```

### 7. Delete Post
Description: Soft deletes a post created by the user.

**EndPoint:**
```
DELETE api/v1/posts/{id}
```

**Authentication:**
Required

**Request Body:**
None

**Response:**
- **Success (200 OK):**
```json
{
  "success": true
}
```

**Example Request:**
```bash
curl -X DELETE https://api.example.com/api/v1/posts/123 \
  -H "Authorization: Bearer <TOKEN>"
```

### 8. Get Details
Description: Retrieves the full post detail, including counters.

**EndPoint:**
```
GET api/v1/posts/{id}
```

**Authentication:**
Optional (adds personalized liked/faved flags)

**Request Body:**
None

**Response:**
- **Success (200 OK):** Returns `PostDetailResponse` JSON.

**Example Request:**
```bash
curl -X GET https://api.example.com/api/v1/posts/123 \
  -H "Authorization: Bearer <TOKEN>"
```

### 9. Fetch Feed
Description: Returns the global feed timeline with pagination.

**EndPoint:**
```
GET api/v1/posts/feed
```

**Authentication:**
Optional

**Request Body:**
None (use `page` and `size` query params)

**Response:**
- **Success (200 OK):** Returns a `FeedPageResponse` JSON.

**Example Request:**
```bash
curl -X GET 'https://api.example.com/api/v1/posts/feed?page=1&size=10' \
  -H "Authorization: Bearer <TOKEN>"
```

### 10. Fetch My Feed
Description: Returns posts created by the authenticated user.

**EndPoint:**
```
GET api/v1/posts/my
```

**Authentication:**
Required

**Request Body:**
None (use `page` and `size` query params)

**Response:**
- **Success (200 OK):** Same as global feed response.

**Example Request:**
```bash
curl -X GET 'https://api.example.com/api/v1/posts/my?page=1&size=10' \
  -H "Authorization: Bearer <TOKEN>"
```