# 🚀 Blog Projesi - Ücretsiz Deployment Rehberi

## Gereksinimler
✅ **Tamamen Ücretsiz** - Kart bilgisi gerekmez
✅ **Supabase** - PostgreSQL veritabanı
✅ **Render** - Spring Boot uygulaması

---

## 📝 Adım 1: Supabase Database Kurulumu

### 1.1 Hesap Oluştur
1. [supabase.com](https://supabase.com) adresine git
2. "Start your project" → GitHub ile giriş yap (ücretsiz)
3. Kart bilgisi istemez ✅

### 1.2 Database Oluştur
1. "New Project" butonuna tıkla
2. Proje bilgilerini gir:
   - **Name**: blog-database (veya istediğin isim)
   - **Database Password**: Güçlü bir şifre seç (KAYDET!)
   - **Region**: Europe (Frankfurt veya yakın bölge)
3. "Create new project" tıkla (1-2 dakika sürer)

### 1.3 Connection String Al
1. Sol menüden **"Project Settings"** (dişli ikonu) → **"Database"**
2. **"Connection string"** bölümünde **"URI"** sekmesini seç
3. `postgresql://postgres:[YOUR-PASSWORD]@...` gibi bir string görürsün
4. `[YOUR-PASSWORD]` yerine şifreni yaz
5. Bu string'i kopyala (DATABASE_URL için kullanacağız)

### 1.4 Connection Pooling (Önemli!)
Render için connection pooling kullanmalısın:
1. Aynı "Database" sayfasında **"Connection Pooler"** bölümünü bul
2. **"Connection pooling"** sekmesinde **"Transaction"** modunu seç
3. Port: `6543` (veya gösterilen port)
4. Bu connection string'i kullan (port 5432 yerine 6543 olacak)

**Örnek:**
```
postgresql://postgres.xxxxx:password@aws-0-eu-central-1.pooler.supabase.com:6543/postgres
```

---

## 📝 Adım 2: GitHub'a Yükle

### 2.1 Git Reposu Oluştur
1. GitHub'da yeni repo oluştur: [github.com/new](https://github.com/new)
2. Repo adı: `spring-blog-app` (veya istediğin isim)
3. Public veya Private seç
4. "Create repository"

### 2.2 Kodu Yükle
Terminalde şu komutları çalıştır:

```bash
# Git başlat (eğer yoksa)
git init

# Dosyaları ekle
git add .
git commit -m "Initial commit for deployment"

# GitHub'a bağla (XXX yerine GitHub kullanıcı adın ve repo adını yaz)
git remote add origin https://github.com/KULLANICI-ADIN/REPO-ADIN.git
git branch -M main
git push -u origin main
```

---

## 📝 Adım 3: Render Deployment

### 3.1 Hesap Oluştur
1. [render.com](https://render.com) adresine git
2. "Get Started for Free" → GitHub ile giriş yap
3. Kart bilgisi istemez ✅

### 3.2 Yeni Web Service Oluştur
1. Dashboard'da **"New +"** → **"Web Service"**
2. GitHub repo'nu bağla:
   - "Connect repository" tıkla
   - GitHub yetkilendirmesini onayla
   - `spring-blog-app` repo'nu seç

### 3.3 Ayarları Yapılandır
**Build & Deploy:**
- **Name**: `spring-blog-app` (veya benzersiz bir isim)
- **Region**: Frankfurt (veya yakın)
- **Branch**: `main`
- **Runtime**: `Java`
- **Build Command**: 
  ```
  ./gradlew build -x test
  ```
- **Start Command**: 
  ```
  java -jar build/libs/*.jar
  ```

**Instance Type:**
- ✅ **Free** seç (kart gerekmez)

### 3.4 Environment Variables Ekle
"Advanced" → "Add Environment Variable":

| Key | Value |
|-----|-------|
| `DATABASE_URL` | Supabase connection string (pooler URL) |
| `DATABASE_USERNAME` | `postgres` |
| `DATABASE_PASSWORD` | Supabase şifresi |
| `SPRING_PROFILES_ACTIVE` | `production` |

**Örnek DATABASE_URL:**
```
jdbc:postgresql://db.xxxxx.supabase.co:6543/postgres?sslmode=require
```

> ⚠️ **ÖNEMLİ**: Supabase URL'ine `?sslmode=require` ekle ve `postgresql://` yerine `jdbc:postgresql://` kullan

### 3.5 Deploy Et
1. "Create Web Service" butonuna tıkla
2. İlk build 5-10 dakika sürebilir
3. Deploy tamamlandığında URL göreceksin: `https://spring-blog-app.onrender.com`

---

## 🎉 Test Et

1. Render URL'ini tarayıcıda aç: `https://SENIN-APP-ADIN.onrender.com`
2. İlk istek 30 saniye sürebilir (cold start)
3. Login/Register sayfalarını test et
4. Supabase Dashboard'dan Table Editor'de verileri görebilirsin

---

## 🔧 Sorun Giderme

### Build Hatası: "Permission Denied"
Gradlew dosyası çalıştırılabilir değilse:
```bash
git update-index --chmod=+x gradlew
git commit -m "Make gradlew executable"
git push
```

### Database Connection Hatası
1. Supabase connection string doğru mu kontrol et
2. `sslmode=require` parametresi var mı?
3. Port `6543` (pooler) kullanıyor musun?
4. `jdbc:postgresql://` prefixi var mı?

### 503 Service Unavailable
- Ücretsiz plan 15 dakika hareketsizlikten sonra uyur
- İlk istekte ~30 saniye bekle (uyanma süresi)

### Logs Kontrol
Render Dashboard → "Logs" sekmesinden hata mesajlarını görebilirsin

---

## 💰 Maliyet Bilgisi

| Servis | Plan | Limit | Maliyet |
|--------|------|-------|---------|
| Supabase | Free | 500MB DB, 2GB transfer | ₺0 |
| Render | Free | 750 saat/ay, 512MB RAM | ₺0 |
| **TOPLAM** | | | **₺0/ay** |

---

## 🔄 Güncelleme Yapmak

Kod değişikliği yaptığında:
```bash
git add .
git commit -m "Değişiklik açıklaması"
git push
```

Render otomatik olarak yeniden deploy eder.

---

## 📚 Faydalı Linkler

- Supabase Dashboard: https://app.supabase.com
- Render Dashboard: https://dashboard.render.com
- Render Logs: Dashboard → Logs sekmesi
- Supabase Table Editor: Dashboard → Table Editor

---

## ⚡ Önemli Notlar

1. **Ücretsiz plan limitleri:**
   - Render: 15 dakika hareketsizlikten sonra uyur
   - Supabase: 500MB veritabanı, 2GB bandwidth
   
2. **Production için öneriler:**
   - Session timeout ayarını kontrol et
   - HTTPS kullan (Render otomatik sağlar)
   - Logları düzenli takip et

3. **Güvenlik:**
   - Şifreleri environment variable'da tut
   - Asla kodda hardcode etme
   - `.env` dosyasını `.gitignore`'a ekle

Başarılar! 🎊
