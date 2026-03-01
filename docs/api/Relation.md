## RELATION API

- Description: RelationController endpoints for follow/unfollow, status, lists, and counters.

### 1. Follow
Description: Current user follows another user.

**EndPoint:**
```
POST api/v1/relation/follow?toUserId={userId}
```

**Authentication:**
Required

**Request Body:**
None

**Response:**
- **Success (200 OK):**
```json
true
```

### 2. Unfollow
Description: Current user unfollows another user.

**EndPoint:**
```
POST api/v1/relation/unfollow?toUserId={userId}
```

**Authentication:**
Required

**Response:**
```json
true
```

### 3. Status
Description: Checks the relationship between current user and target.

**EndPoint:**
```
GET api/v1/relation/status?toUserId={userId}
```

**Authentication:**
Required

**Response:**
```json
{
  "following": true,
  "followedBy": false,
  "mutual": false
}
```

### 4. Following List
Description: Lists profiles the specified user is following.

**EndPoint:**
```
GET api/v1/relation/following
```

**Authentication:**
Not required

**Query Parameters:**
- `userId` (required)
- `limit` (default 20)
- `offset` (default 0)
- `cursor` (optional)

**Response:**
```json
[
  {
    "id": 24,
    "nickname": "Bob",
    "avatar": "https://cdn.example.com/bob.png",
    "bio": "Coffee lover",
    "zgId": "bob24",
    "gender": "MALE",
    "birthday": "1990-01-02",
    "school": "CMU",
    "phone": "+123456789",
    "email": "bob@example.com",
    "tagJson": "{}"
  }
]
```

### 5. Followers List
Description: Lists profiles that follow the specified user.

**EndPoint:**
```
GET api/v1/relation/followers
```

**Authentication:**
Not required

**Query Parameters:** Same as following list.

**Response:** Array of `ProfileResponse`.

### 6. Counters
Description: Returns cached counts (followings, followers, posts, liked posts, faved posts).

**EndPoint:**
```
GET api/v1/relation/counters?userId={userId}
```

**Authentication:**
Not required

**Response:**
```json
{
  "followings": 120,
  "followers": 85,
  "posts": 34,
  "likedPosts": 210,
  "favedPosts": 12
}
```
