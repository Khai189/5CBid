# CS62 Final Project Backend

5CBid is a Spring Boot + PostgreSQL backend for a 5C student bidding marketplace. It supports account creation, JWT login, item listing, bidding, highest-bid lookup, bid removal, and recommendation/feed generation based on bidding history. A typical run looks like this: a seller creates an auctioneer account, posts a listing, a bidder signs in and places a bid, and the recommendation/feed endpoints return related items based on the bidder's activity.

## Repository Notes

- Backend code lives in `demo/`
- The separate Next.js frontend lives in `/Users/husamkm/CS62FinalFrontend`
- There is no `/lib` folder of committed `.jar` files because this project uses Maven dependencies from `demo/pom.xml`

## How To Run The Code

1. Make sure Java 21 and Maven are installed.
2. Make sure PostgreSQL is running.
3. Set environment variables.
4. Start the backend from the `demo/` folder.

```bash
cd demo
export PGHOST=localhost
export PGPORT=5432
export PGDATABASE=ccbid
export PGUSER=postgres
export PGPASSWORD=postgres
export APP_SECURITY_JWT_SECRET=replace-with-a-long-random-secret-at-least-32-characters
export APP_SECURITY_ALLOWED_ORIGINS=http://localhost:3000
mvn spring-boot:run
```

You can also use direct Spring datasource variables instead of the `PG*` variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ccbid
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
```

## Environment Variables


- `APP_SECURITY_JWT_SECRET`
- Either:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
- Or:
  - `PGHOST`
  - `PGPORT`
  - `PGDATABASE`
  - `PGUSER`
  - `PGPASSWORD`

Optional:

- `APP_SECURITY_ALLOWED_ORIGINS`
- `APP_SECURITY_JWT_EXPIRATION_SECONDS`
- `PORT`

## External Libraries

This project uses Maven-managed libraries, so there are no manual `.jar` installation steps. Main libraries include:

- Spring Boot Web MVC
- Spring Boot Data JPA
- Spring Security
- Spring OAuth2 Resource Server
- PostgreSQL JDBC Driver
- H2 for tests
- Lombok

Install them by running:

```bash
cd demo
mvn clean install
```

## REST API Methods (Methods that require POST, GET, DELETE, PUT, etc)

All protected endpoints require a JWT bearer token from `/auth/register` or `/auth/login`.

### `POST /auth/register`

- Input: `RegisterRequest`
- Output: `AuthSessionResponse`
- Description: Creates a new bidder or auctioneer account and returns a JWT session.

Example request:

```json
{
  "username": "maya123",
  "email": "maya@students.pomona.edu",
  "password": "secret123",
  "displayName": "Maya",
  "role": "BIDDER"
}
```

Example response shape:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "user": {
    "username": "maya123",
    "email": "maya@students.pomona.edu",
    "displayName": "Maya",
    "role": "BIDDER",
    "profileId": "maya123"
  }
}
```

### `POST /auth/login`

- Input: `AuthLoginRequest`
- Output: `AuthSessionResponse`
- Description: Logs in an existing user and returns a JWT session.

Usage example:

```json
{
  "username": "maya123",
  "password": "secret123"
}
```

### `GET /auth/me`

- Input: Bearer token
- Output: `AuthResponse`
- Description: Returns the currently authenticated user's profile information.

Usage example:

```http
GET /auth/me
Authorization: Bearer <token>
```

### `GET /items/all`

- Input: Bearer token, optional query params `query`, `auctioneerId`, `minPrice`, `maxPrice`, `condition`
- Output: `Iterable<BidItem>`
- Description: Returns all currently listed non-archived items, optionally filtered by search text, seller, opening-bid range, and condition.

Usage example:

```http
GET /items/all?query=lamp&minPrice=5&maxPrice=25&condition=USED
Authorization: Bearer <token>
```

### `GET /items/{itemId}`

- Input: `itemId`
- Output: `BidItem`
- Description: Returns one item by ID.

Usage example:

`GET /items/lamp-101`

### `POST /bid/list`

- Input: `ListItemRequest`
- Output: `ListItemResponse`
- Description: Lets an auctioneer post a listing. The backend generates the listing ID automatically.

Usage example:

```json
{
  "itemName": "Desk Lamp",
  "startingPrice": 15,
  "description": "Warm light for a dorm desk.",
  "condition": "USED"
}
```

Expected output:

```json
{
  "itemId": "desk-lamp-a1b2c3d4",
  "itemName": "Desk Lamp",
  "auctioneerId": "seller123",
  "auctioneerName": "Maya",
  "message": "Listed desk-lamp-a1b2c3d4 (Desk Lamp) by auctioneer seller123"
}
```

### `POST /bid/{itemId}`

- Input: `itemId`, `PlaceBidRequest`
- Output: `String`
- Description: Places a new bid for the signed-in bidder.

Usage example:

`POST /bid/lamp-101`

```json
{
  "amount": 22
}
```

Expected output:

`Bid placed: bidder=maya123 amount=22 item=lamp-101 | currentHighest=22 by maya123`

### `GET /bid/{itemId}/highest`

- Input: `itemId`
- Output: `String`
- Description: Returns the highest bid and bidder for an item.

Usage example:

`GET /bid/lamp-101/highest`

Expected output:

`Highest on lamp-101: maya123 @ 22`

### `GET /bid/active`

- Input: Bearer token
- Output: `Iterable<ActiveBidSummaryResponse>`
- Description: Returns active bid activity for the current user. Bidders see the listings they are bidding on, auctioneers see their live listings, and admins see all live bid summaries.

Usage example:

```http
GET /bid/active
Authorization: Bearer <token>
```

### `GET /bid/history`

- Input: Bearer token
- Output: `Iterable<BidHistorySummaryResponse>`
- Description: Returns past bid history for bidders, archived auction history for auctioneers, and all archived history for admins.

Usage example:

```http
GET /bid/history
Authorization: Bearer <token>
```

### `GET /bid/expired`

- Input: Bearer token
- Output: `Iterable<BidHistorySummaryResponse>`
- Description: Returns expired listing outcomes, including final highest bidder, final bid amount, bid count, and closing timestamp.

Usage example:

```http
GET /bid/expired
Authorization: Bearer <token>
```

### `DELETE /bid/{itemId}`

- Input: `itemId`
- Output: `String`
- Description: Removes the signed-in bidder's active bid for an item.

Usage example:

`DELETE /bid/lamp-101`

Expected output:

`Removed bid by maya123 on lamp-101`

### `DELETE /bid/{itemId}/{bidderId}`

- Input: `itemId`, `bidderId`
- Output: `String`
- Description: Admin-only cleanup route for removing another bidder's bid.

Usage example:

`DELETE /bid/lamp-101/maya123`

### `GET /bidders/all`

- Input: Bearer token
- Output: `Iterable<Bidder>`
- Description: Returns all bidder profiles.

Usage example:

`GET /bidders/all`

### `GET /bidders/{bidderId}`

- Input: `bidderId`
- Output: `Bidder`
- Description: Returns one bidder by ID.

Usage example:

`GET /bidders/maya123`

### `POST /bidders/add/{bidderId}/{name}`

- Input: `bidderId`, `name`
- Output: `String`
- Description: Admin-only route for manually creating a bidder record.

Usage example:

`POST /bidders/add/maya123/Maya`

### `GET /rec/{bidderId}/{itemId}/{totalRecs}`

- Input: `bidderId`, `itemId`, `totalRecs`
- Output: `List<BidItem>`
- Description: Returns recommended items using the item graph and shortest-path ranking.

Usage example:

`GET /rec/maya123/lamp-101/3`

### `GET /feed/{bidderId}`

- Input: `bidderId`
- Output: `List<BidItem>`
- Description: Returns a feed of items related to the bidder's history.

Usage example:

`GET /feed/maya123`

## Public Classes, Constructors, And Methods

### `ItemBid`

Constructor usage:

- `new ItemBid()` creates an empty bid wrapper.
- `new ItemBid(auctioneer, item)` creates a bid wrapper around one listing.

Public methods:

- `addBid(String bidderId, int bidAmount)`
  - Input: bidder ID and amount
  - Output: none
  - Description: adds or updates a bidder's bid in the heap/map structure
  - Example: `addBid("maya123", 22)`

- `removeBid(String bidderId)`
  - Input: bidder ID
  - Output: none
  - Description: removes a bidder's active bid
  - Example: `removeBid("maya123")`

- `getHighestBid()`
  - Input: none
  - Output: `int`
  - Description: returns the current top bid or `-1`
  - Example: `getHighestBid() = 22`

- `getHighestBidder()`
  - Input: none
  - Output: `String`
  - Description: returns the ID of the highest bidder or `null`
  - Example: `getHighestBidder() = "maya123"`

### `ItemGraph<T>`

Constructor usage:

- `new ItemGraph<String>()` creates an empty weighted graph keyed by item IDs

Public methods:

- `addVertex(T vertex)`
  - Example: `addVertex("lamp-101")`

- `clear()`
  - Example: `clear()`

- `addEdge(T from, T to, int weight, boolean bidirectional)`
  - Description: adds a weighted edge, optionally both directions
  - Example: `addEdge("lamp-101", "chair-202", 50, true)`

- `getVertexCount()`
  - Example: `getVertexCount() = 2`

- `getEdgeCount()`
  - Example: `getEdgeCount() = 2`

- `hasVertex(T vertex)`
  - Example: `hasVertex("lamp-101") = true`

- `hasEdge(T from, T to)`
  - Example: `hasEdge("lamp-101", "chair-202") = true`

- `getNeighbors(T vertex)`
  - Example: `getNeighbors("lamp-101") = {"chair-202"=50}`

- `getEdgeWeight(T from, T to)`
  - Example: `getEdgeWeight("lamp-101", "chair-202") = 50`

- `getShortestPaths(T source)`
  - Description: runs Dijkstra-style shortest path ranking from one source vertex
  - Example: `getShortestPaths("lamp-101") = {"chair-202"=50, "bike-303"=80}`

## Test Commands

Run the backend tests:

```bash
cd demo
mvn test
```

## Example Application Flow

1. Register a bidder with `/auth/register`
2. Register an auctioneer with `/auth/register`
3. Use the auctioneer token to call `/bid/list` and save the generated `itemId`
4. Use the bidder token to call `/bid/{itemId}`
5. Call `/bid/{itemId}/highest`
6. Call `/bid/active`, `/bid/history`, or `/bid/expired`
7. Call `/feed/{bidderId}` or `/rec/{bidderId}/{itemId}/{totalRecs}`
