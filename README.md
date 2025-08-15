# 📰 Contextual News Retrieval System (Spring Boot + LLM Integration)

This backend system allows intelligent querying, filtering, ranking, and enrichment of news articles using a Large Language Model (LLM) like OpenAI or Cohere. It simulates multiple API endpoints to reflect real-world use cases, including **category filtering**, **relevance score ranking**, **source-based search**, **proximity search**, and a **trending feed** based on user interaction data.

---

## 🧠 Features Overview

* ✅ Load and store JSON-based news data into MySQL DB.
* ✅ Accept natural language queries.
* ✅ Extract **intent**, **entities**, **keywords**, **locations**, and **relevance thresholds** using LLM.
* ✅ Filter and rank results based on inferred intent.
* ✅ Generate LLM-based **summaries** for enrichment.
* ✅ Provide a **trending feed** via simulated user events.
* ✅ Modular design using Spring Boot, JPA, and RESTful practices. 

---

## 📦 Project Setup

### 1. Clone Repository

```bash
git clone https://github.com/your-username/news-retrieval-system.git
cd news-retrieval-system
```

### 2. Configure `application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/newsdb
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
openai.api.key=your_openai_or_cohere_api_key
```
add your api keys of any llm model (then only /query will work and data enrichment call to LLM works)

### 3. Run the App

```
use the Run Application button in intellij on the right corner
```

---

## 📟 JSON Data Structure

Each article looks like this (stored in `news_articles` table):

```json
{
  "id": "uuid",
  "title": "Article Title",
  "description": "Description of article...",
  "url": "https://example.com/article",
  "publication_date": "2025-03-24T11:08:11",
  "source_name": "DW News",
  "category": ["Technology"],
  "relevance_score": 0.85,
  "latitude": 21.75,
  "longitude": 80.56
}
```

---

## 🧠 LLM Integration

### LLM Prompt Template

```text
You are an intelligent assistant helping to interpret news search queries.

Extract:
1. intent: [category, score, source, search, nearby]
2. entities: proper nouns (people/places/orgs)
3. location: e.g. "Palo Alto"
4. keywords: core terms
5. relevanceScoreThreshold: (optional, only if query has "top", "important", etc.)

Query: "%s"
```

### Sample LLM Output

```json
{
  "intent": ["source", "category", "nearby"],
  "entities": ["Elon Musk", "Twitter", "Palo Alto"],
  "location": ["Palo Alto"],
  "keywords": ["Elon Musk", "Twitter", "Tech"],
  "relevanceScoreThreshold": 0.75
}
```

---

## 🔎 Main API Endpoints

All API endpoints use the base path:

```http
/api/v1/news
```

### 1. `GET /query`

**Description:** Processes the user query using LLM, then fetches intent, keywords, relevanceScore, Location, entities. After processing using normalization of all intents, filtering and ranking returns top 5 relevant articles.

**Example:**

```http
GET /api/v1/news/query?query=Top Elon Musk updates from Reuters near New York
```

**Response:**

```json
{
  "articles": [
    {
      "title": "Tesla News",
      "description": "Elon Musk announced...",
      "url": "https://...",
      "publication_date": "2025-07-01T10:00:00",
      "source_name": "Reuters",
      "category": ["Business"],
      "relevance_score": 0.92,
      "llm_summary": "This article summarizes Elon Musk's recent moves...",
      "latitude": 40.71,
      "longitude": -74.00
    }
  ]
}
```

---

## 📚 Simulated Endpoints

### 2. `GET /category`

```http
GET /api/v1/news/category?category=technology
```

### 3. `GET /source`

```http
GET /api/v1/news/source?source=bbc
```

### 4. `GET /score`

```http
GET /api/v1/news/score?threshold=0.8
```

### 5. `GET /search`

```http
GET /api/v1/news/search?query=Elon Musk Twitter acquisition
```

### 6. `GET /nearby`

```http
GET /api/v1/news/nearby?lat=40.7128&lon=-74.0060&radius=50
```

---

## 📈 Bonus: Trending Feed

### 7. `GET /trending`

```http
GET /api/v1/news/trending?lat=40.7128&lon=-74.0060&limit=5
```

### Trending Score Factors:

* ✅ Type of user interaction (clicks > views)
* ✅ Recency of interaction
* ✅ Distance from user location

### Simulated Event Model:

```json
{
  "userId": "u1",
  "articleId": "a123",
  "eventType": "click",
  "timestamp": "2025-07-23T10:00:00",
  "lat": 40.7128,
  "lon": -74.0060
}
```

---

## 🧠 Ranking Strategy

| Intent          | Ranking Logic                  |
| --------------- | ------------------------------ |
| category/source | Most recent (publication date) |
| score           | Highest relevance score        |
| search          | Text match + relevance         |
| nearby          | Geographical closeness         |
| multiple        | Composite scoring strategy     |

### Composite Score:

```java
score = 0.5 * normalize(score) +
        0.3 * recencyScore +
        0.2 * keywordMatch +
        0.2 * distanceScore;
```

---

## 🧹 Filtering Logic

Filtering is applied based on LLM-inferred intents:

* **Category** → Match keywords/entities with article categories
* **Score** → Filter by `relevanceScoreThreshold`
* **Search** → Keyword presence in title/description

> AND logic is used 

---

## 📌 Output Format

Each article includes:

* title
* description
* url
* publication\_date
* source\_name
* category
* relevance\_score
* llm\_summary
* latitude, longitude





## 🧠 Example Use Case

**Query:**

> "Top SpaceX news from Reuters in the past week near San Francisco"

**LLM Intent:** `["source", "category", "score", "nearby"]`

**Filtering:**

* Source: Reuters
* Category: Technology/Space
* Score: ≥ 0.75
* Distance ≤ 50km from San Francisco

---


## 📈 Global Exception handling through ControllerAdvice

## 🛀 Tech Stack

* Java 17
* Spring Boot 3.x
* Hibernate / JPA
* MySQL
* OpenAI / Cohere LLM API
* Haversine geo logic
* Jackson / RestTemplate
* RestTemplate (for extenal api usages)

---

