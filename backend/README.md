# ZestWear — Backend (Spring Boot)

Một skeleton backend Spring Boot minimal cho dự án ZestWear.

Quick start

- Build:

```bash
mvn -f backend clean package
```

- Run (development):

```bash
mvn -f backend spring-boot:run
```

- H2 console: http://localhost:8081/h2-console (JDBC URL: `jdbc:h2:mem:zestdb`)
- API base: `http://localhost:8081/api/products`

Notes

- Mặc định backend lắng nghe trên port `8081`.
- Không tự chạy backend nếu bạn không muốn (user preference: do_not_run_backend = true).
- Frontend có thể được copy từ dự án Dotnet_Ecomerce/frontend (xem hướng dẫn trong thư mục frontend/).

## Upgrade Notes

- Upgraded Spring Boot to **3.5.0** (2026-04-14).
- Build verified with JDK 25 and Maven 3.9.14 on this machine. If you need strict Java 25 support across environments, consider installing Maven 4.x or adding a Maven wrapper (`mvnw`) pinned to a Maven 4.x distribution.

