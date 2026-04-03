# 🚀 CI/CD Deployment Guide

## **📋 Overview**
This guide explains how to deploy the User Service to free cloud platforms using automated CI/CD pipelines.

## **🔧 Prerequisites**
- GitHub repository with the code
- Railway account (free tier)
- Docker installed locally

---

## **🚀 Option 1: Railway (Recommended - Easiest)**

### **Step 1: Set up Railway Account**
1. Go to [railway.app](https://railway.app)
2. Sign up with GitHub
3. Get $5/month free credit

### **Step 2: Create Railway Project**
```bash
# Install Railway CLI
npm install -g @railway/cli

# Login to Railway
railway login

# Create new project
railway new
```

### **Step 3: Deploy**
```bash
# Deploy from GitHub
railway up

# Or connect GitHub repo for auto-deployment
railway link
```

### **Step 4: Set Environment Variables**
```bash
# Production database (Railway provides free PostgreSQL)
railway variables set DATABASE_URL=postgresql://user:pass@host:5432/db

# Application settings
railway variables set SPRING_PROFILES_ACTIVE=prod
railway variables set PORT=8080
```

### **Step 5: Get Deployment URL**
```bash
railway open
```

---

## **🚀 Option 2: Vercel (Alternative)**

### **Step 1: Install Vercel CLI**
```bash
npm install -g vercel
```

### **Step 2: Deploy**
```bash
vercel --prod
```

### **Step 3: Configure Environment Variables**
```bash
vercel env add SPRING_PROFILES_ACTIVE production
vercel env add DATABASE_URL production
```

---

## **🚀 Option 3: Render (Alternative)**

### **Step 1: Create Render Account**
1. Go to [render.com](https://render.com)
2. Sign up with GitHub
3. Get 750 hours free/month

### **Step 2: Create Web Service**
1. Connect GitHub repository
2. Choose "Docker" runtime
3. Set build command: `./gradlew build`
4. Set start command: `java -jar build/libs/*.jar`

### **Step 3: Environment Variables**
```
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=postgresql://...
PORT=8080
```

---

## **🔧 GitHub Actions CI/CD**

### **Automatic Pipeline Features:**
- ✅ **Testing**: Automated unit and integration tests
- ✅ **Building**: Docker image creation
- ✅ **Security**: Trivy vulnerability scanning
- ✅ **Deployment**: Automatic deployment to Railway
- ✅ **Monitoring**: Health checks and rollback

### **Required GitHub Secrets:**
```yaml
RAILWAY_TOKEN: your_railway_api_token
RAILWAY_SERVICE_ID: your_railway_service_id
DATABASE_URL: production_database_url
REDIS_URL: production_redis_url
```

### **Pipeline Triggers:**
- **Push to main**: Full CI/CD pipeline
- **Pull requests**: Testing and security scan only
- **Feature branches**: Testing only

---

## **🌐 Free Tier Limitations**

### **Railway:**
- ✅ $5/month credit
- ✅ Free PostgreSQL database
- ✅ Automatic SSL
- ✅ Custom domains
- ⚠️ Credit resets monthly

### **Vercel:**
- ✅ Unlimited hobby projects
- ✅ 100GB bandwidth/month
- ✅ Automatic SSL
- ✅ Global CDN
- ⚠️ 10-second function timeout

### **Render:**
- ✅ 750 hours/month
- ✅ Free PostgreSQL
- ✅ Automatic SSL
- ✅ Private repositories
- ⚠️ Sleeps after 15 minutes inactivity

---

## **📊 Monitoring & Health Checks**

### **Health Endpoints:**
```bash
# Health check
GET https://your-app.railway.app/actuator/health

# Metrics
GET https://your-app.railway.app/actuator/metrics

# Application info
GET https://your-app.railway.app/actuator/info
```

### **Monitoring Setup:**
- ✅ **Health checks**: Automatic railway monitoring
- ✅ **Logs**: Railway built-in logging
- ✅ **Alerts**: Email notifications on failures
- ✅ **Metrics**: Prometheus endpoints available

---

## **🔄 Deployment Workflow**

### **Development:**
1. Push to `feature/*` branch
2. Run tests automatically
3. Create pull request
4. Security scan runs

### **Production:**
1. Merge to `main` branch
2. Full CI/CD pipeline runs
3. Docker image built and pushed
4. Automatic deployment to Railway
5. Health checks verify deployment

---

## **🛠️ Local Testing**

### **Test Docker Build:**
```bash
# Build locally
docker build -t user-service .

# Run locally
docker run -p 8080:8080 user-service
```

### **Test Production Config:**
```bash
# Run with production profile
./gradlew bootRun --args="--spring.profiles.active=prod"
```

---

## **📱 Mobile App Ready**

The deployed API provides endpoints for mobile apps:

```json
{
  "base_url": "https://your-app.railway.app/api/v1",
  "endpoints": {
    "users": "/users",
    "health": "/actuator/health"
  }
}
```

---

## **🎯 Quick Start**

### **Deploy in 5 Minutes:**
```bash
# 1. Clone and setup
git clone <your-repo>
cd user-service

# 2. Install Railway CLI
npm install -g @railway/cli
railway login

# 3. Deploy
railway up

# 4. Set environment
railway variables set SPRING_PROFILES_ACTIVE=prod

# 5. Get URL
railway open
```

---

## **🆘 Troubleshooting**

### **Common Issues:**
- **Build failures**: Check Java version (requires Java 21)
- **Database connection**: Verify DATABASE_URL format
- **Port conflicts**: Railway uses PORT env var automatically
- **Memory issues**: Reduce JVM heap size in production

### **Debug Commands:**
```bash
# Check logs
railway logs

# Check status
railway status

# Redeploy
railway up
```

---

## **📈 Next Steps**

After successful deployment:

1. **Set up custom domain** (Railway supports this)
2. **Configure monitoring** (Prometheus + Grafana)
3. **Set up alerts** (Email/Slack notifications)
4. **Add authentication** (JWT/OAuth2)
5. **Scale horizontally** (Railway auto-scaling)

---

## **🎉 Success!**

Your User Service is now:
- ✅ **Automatically tested** on every push
- ✅ **Securely scanned** for vulnerabilities
- ✅ **Automatically deployed** to production
- ✅ **Monitored** with health checks
- ✅ **Globally accessible** via HTTPS

**🚀 Your production-ready microservice is live!**
