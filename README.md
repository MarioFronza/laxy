# Laxy API - Available Requests & Responses

## 1. Authentication

### 🔹 Sign Up
- **POST** /users
- **Request Body:**
  {
  "user": {
  "username": "exampleUser",
  "email": "user@example.com",
  "password": "password123"
  }
  }
- **Response:**
  {
  "user": {
  "email": "user@example.com",
  "token": "jwt_token_here",
  "username": "exampleUser"
  }
  }

### 🔹 Sign In
- **POST** /users/login
- **Request Body:**
  {
  "user": {
  "email": "user@example.com",
  "password": "password123"
  }
  }
- **Response:**
  {
  "user": {
  "email": "user@example.com",
  "token": "jwt_token_here",
  "username": "exampleUser"
  }
  }

---

## 2. Languages

### 🔹 Get All Languages
- **GET** /languages
- **Headers:** Authorization: Bearer <TOKEN>
- **Response:**
  {
  "languages": [
  {
  "id": 1,
  "name": "English",
  "code": "en"
  },
  {
  "id": 2,
  "name": "Spanish",
  "code": "es"
  }
  ]
  }

### 🔹 Get Subjects by Language ID
- **GET** /languages/{id}/subjects
- **Headers:** Authorization: Bearer <TOKEN>
- **Response:**
  {
  "subjects": [
  {
  "id": 1,
  "name": "History",
  "description": "Learn about world history."
  },
  {
  "id": 2,
  "name": "Mathematics",
  "description": "Advanced algebra and calculus."
  }
  ]
  }

---

## 3. Subjects

### 🔹 Get All Subjects
- **GET** /subjects
- **Headers:** Authorization: Bearer <TOKEN>
- **Response:**
  {
  "subjects": [
  {
  "id": 1,
  "name": "History",
  "description": "World History",
  "language": "English"
  },
  {
  "id": 2,
  "name": "Physics",
  "description": "Fundamentals of physics",
  "language": "Spanish"
  }
  ]
  }

### 🔹 Get Subject by ID
- **GET** /subjects/{id}
- **Headers:** Authorization: Bearer <TOKEN>
- **Response:**
  {
  "subject": {
  "id": 1,
  "name": "History",
  "description": "World History",
  "language": "English"
  }
  }

---

## 4. Quizzes

### 🔹 Get All Quizzes for User
- **GET** /quizzes
- **Headers:** Authorization: Bearer <TOKEN>
- **Response:**
  {
  "quizzes": [
  {
  "id": 101,
  "subject": "Mathematics",
  "totalQuestions": 10,
  "status": "completed",
  "createdAt": "2024-03-03T12:00:00Z"
  }
  ]
  }

### 🔹 Get Quiz Questions
- **GET** /quizzes/{quizId}/questions
- **Headers:** Authorization: Bearer <TOKEN>
- **Response:**
  [
  {
  "id": 1,
  "description": "What is 2 + 2?",
  "options": [
  { "id": 11, "description": "3", "referenceNumber": 0 },
  { "id": 12, "description": "4", "referenceNumber": 1 },
  { "id": 13, "description": "5", "referenceNumber": 2 },
  { "id": 14, "description": "6", "referenceNumber": 3 }
  ]
  }
  ]

### 🔹 Get Options for a Question
- **GET** /quizzes/{quizId}/questions/{questionId}/options
- **Headers:** Authorization: Bearer <TOKEN>
- **Response:**
  [
  { "id": 11, "description": "3", "referenceNumber": 0 },
  { "id": 12, "description": "4", "referenceNumber": 1 },
  { "id": 13, "description": "5", "referenceNumber": 2 },
  { "id": 14, "description": "6", "referenceNumber": 3 }
  ]

### 🔹 Create a New Quiz
- **POST** /quizzes
- **Headers:** Authorization: Bearer <TOKEN>
- **Request Body:**
  {
  "quiz": {
  "subjectId": 1,
  "totalQuestions": 5
  }
  }
- **Response:**
  {
  "quiz": {
  "id": 201,
  "totalQuestions": 5
  }
  }
