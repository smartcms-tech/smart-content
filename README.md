# 🧠 smart-content

`smart-content` is the AI-powered content microservice in the **SmartCMS** ecosystem. It manages articles, blog posts, and other content with full versioning support, AI-generated metadata, and flexible scheduling features. Built with a microservices-first mindset, it is containerized, CI/CD ready, and cloud-native.

---

## 🚀 Features

- CRUD APIs for content management
- Versioning using a separate `History` document
- AI-generated summaries, tags, and SEO metadata
- Full-text search support (via Elasticsearch)
- MongoDB as the primary content store
- Dockerized for easy deployment
- GitHub Actions for CI/CD
- Infrastructure-as-Code via Terraform

---

## 🛠️ Tech Stack

- Java 21 + Spring Boot 3.x
- MongoDB / DocumentDB (NoSQL)
- Gradle (Kotlin DSL)
- Docker
- GitHub Actions (CI/CD)
- Terraform + AWS ECS Fargate
- Optional: Redis, Kafka, OpenAI, Elasticsearch

---

## 🧪 Getting Started

### 🐳 Option 1: Run with Docker

```bash
docker build -t smart-content .
docker run -p 8080:8080 smart-content
````
The service will be available at: [http://localhost:8080](http://localhost:8080)

---

### 💻 Option 2: Run Locally with Gradle

Make sure MongoDB is running locally or update `application.yml` to point to your Mongo Atlas URI.

```bash
./gradlew bootRun
```

---

## ✅ Useful Gradle Commands

```bash
# Clean and build the project
./gradlew clean build

# Run tests
./gradlew test

# Run the app
./gradlew bootRun
```

---

## 📦 Docker Commands

```bash
# Build the image
docker build -t smart-content .

# Run the container
docker run -p 8080:8080 smart-content
```

---

## 🚀 CI/CD with GitHub Actions

GitHub Actions is used for validating, deploying, and managing preview environments:

| Workflow File              | Purpose                                 |
|----------------------------|-----------------------------------------|
| `.github/workflows/ci.yml` | Runs on PRs for testing, linting, scan  |
| `.github/workflows/cd.yml` | Manual production deploy via ECS        |
| `.github/workflows/preview.yml` | Optional: Deploy preview envs per PR |

---

## ☁️ Infrastructure as Code (Terraform)

This repo includes infrastructure code to provision MongoDB, ECS services, and related cloud resources.

| Path                        | Description                          |
|-----------------------------|--------------------------------------|
| `infra/modules/mongodb/`    | MongoDB (Atlas or DocumentDB) module |
| `infra/modules/ecs/`        | ECS Fargate setup                    |
| `infra/environments/dev/`   | Dev-specific infra configs           |
| `infra/environments/prod/`  | Production infra configs             |

> Use standard Terraform commands inside the environment folders:

```bash
terraform init
terraform plan
terraform apply
```

---

## 🔐 Security

- All write operations require a valid JWT.
- Auth is handled at the `smartGateway` level.
- Gateway adds user/org info in headers for service-level authorization.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

## 🙋 Contributing

We welcome contributions! Please:
- Fork the repo
- Create a feature branch
- Open a PR describing your changes

Let’s build SmartCMS together 🚀

---
```

Let me know if you'd like this as a downloadable `README.md` file or want help wiring up GitHub Actions next!
