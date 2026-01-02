# Spring Boot Blog Uygulaması

Bu proje, **Spring Boot** ve **Thymeleaf** kullanılarak geliştirilmiş, kullanıcıların makale yayınlayabileceği, yorum yapabileceği ve etkileşimde bulunabileceği tam kapsamlı bir blog uygulamasıdır.

## 🚀 Özellikler

Proje dosya yapısı ve konfigürasyonlara göre aşağıdaki özellikleri içerir:

* **Kullanıcı Yönetimi:**
    * Kayıt Ol ve Giriş Yap (Spring Security)
    * Şifre Sıfırlama ve "Şifremi Unuttum"
    * Profil Yönetimi ve Ayarlar
* **Blog İçerikleri:**
    * Makale Yazma, Düzenleme ve Silme (Taslaklar ve Yayınlananlar)
    * Makale Okuma ve Listeleme
    * Kategoriler/Konular (Topics)
* **Etkileşim:**
    * Yorum Yapma
    * Puanlama/Oylama (Rating)
    * Yer İmlerine Ekleme (Bookmarks)
* **Güvenlik:** Rol tabanlı yetkilendirme ve güvenli oturum yönetimi.

## 🛠️ Teknolojiler

* **Dil:** Java 21
* **Framework:** Spring Boot 3.5.7
* **Veritabanı:** PostgreSQL
* **Frontend:** Thymeleaf, HTML5, CSS
* **Güvenlik:** Spring Security 6
* **Araçlar:** Gradle, Lombok, Docker

## ⚙️ Kurulum ve Çalıştırma

Projeyi yerel makinenizde çalıştırmak için aşağıdaki adımları izleyin.

### Gereksinimler
* Java 21 JDK
* PostgreSQL

### 1. Projeyi Klonlayın
```bash
git clone [https://github.com/mucahit-topcuoglu/spring-blog-app.git](https://github.com/mucahit-topcuoglu/spring-blog-app.git)
cd spring-blog-app
