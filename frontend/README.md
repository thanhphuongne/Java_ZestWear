# ZestWear — Frontend

Hướng dẫn nhanh để lấy frontend giống dự án Dotnet_Ecomerce:

Nếu muốn dùng nguyên frontend từ dự án Dotnet_Ecomerce (Next.js):

PowerShell:

```powershell
Copy-Item -Recurse -Force "Dotnet_Ecomerce\frontend" "Java_ZestWear\frontend"
```

Bash (Git Bash, WSL):

```bash
cp -r "Dotnet_Ecomerce/frontend" "Java_ZestWear/frontend"
```

Sau khi copy, chuyển vào thư mục frontend và cài phụ thuộc:

```bash
cd Java_ZestWear/frontend
npm install
npm run dev
```

Cập nhật cấu hình API (nếu cần) để trỏ tới backend Spring Boot: `http://localhost:8081`.

Nếu muốn scaffold frontend mới thay vì copy, cho tôi biết (tôi sẽ tạo một Next.js starter tương thích).
