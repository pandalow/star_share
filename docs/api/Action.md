## ACTION API

- Description: ActionController endpoints for like/fav toggles.

### 1. Like
Description: Marks the entity as liked by the authenticated user.

**EndPoint:**
```
POST api/v1/action/like
```

**Authentication:**
Required

**Request Body:**
```json
{
  "entityType": "post",
  "entityId": "1234567890"
}
```

**Response:**
- **Success (200 OK):**
```json
{
  "changed": true,
  "liked": true
}
```

### 2. Unlike
Description: Removes the like from the entity.

**EndPoint:**
```
POST api/v1/action/unlike
```

**Authentication:**
Required

**Request Body:** Same as Like.

**Response:**
```json
{
  "changed": true,
  "liked": false
}
```

### 3. Fav
Description: Marks the entity as favorited.

**EndPoint:**
```
POST api/v1/action/fav
```

**Authentication:**
Required

**Request Body:** Same as Like.

**Response:**
```json
{
  "changed": true,
  "faved": true
}
```

### 4. Unfav
Description: Removes the favorite mark from the entity.

**EndPoint:**
```
POST api/v1/action/unfav
```

**Authentication:**
Required

**Request Body:** Same as Like.

**Response:**
```json
{
  "changed": true,
  "faved": false
}
```
