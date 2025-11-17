# Quick Reference Guide - Files & Scripts

## Docker Compose Files

**docker-compose.yml**
→ Local machine only | Builds from source | devprod profile | MySQL + localhost Stripe | Used by local demo/validation scripts

**docker-compose.remote.yml**
→ VM production only | Pulls from Docker Hub | devprod profile | MySQL + VM IP Stripe | Used by VM deployment & CI/CD

---

## Bash Scripts

**validate-cicd-locally.sh**
→ Local machine | Full CI/CD simulation | Builds JAR → runs tests + coverage → builds Docker image → starts containers → health check → pushes to Docker Hub → deploys to VM | Complete pipeline validation

**v-demo-setup-(local).sh**
→ Local machine | Quick demo launcher (builds locally) | Builds JAR (skips tests) → starts containers → prompts for Stripe CLI → opens browser → waits for 'exit' | Interactive classroom demo with local build

**v-demo-setup(with-dockerhub-pull).sh**
→ Local machine | Quick demo launcher (pulls from Docker Hub) | Pulls latest image from Docker Hub → starts containers → prompts for Stripe CLI → opens browser → waits for 'exit' | Demo with pre-built production image

**vm-demo-launcher-(GCS).sh**
→ VM only (via SSH) | Production demo launcher | Pre-flight checks (Docker, port forwarding, git) → pulls image → starts containers → health check → displays public URL → waits for 'exit' | Single command VM demo setup
→ Access VM: https://console.cloud.google.com/compute/instances

**backup_code.sh**
→ Local machine | Creates timestamped backup of entire project directory | Used before major changes

---

## Profile Summary

- **No profile / dev**: H2 in-memory, localhost, local Stripe webhook
- **devprod**: MySQL persistent, localhost, local Stripe webhook
- **prod**: MySQL persistent, VM IP, production Stripe webhook

---

## Quick Decision Tree

**Want to demo locally with payments (build from source)?**
→ Run `v-demo-setup-(local).sh` + Stripe CLI

**Want to demo locally with payments (use production image)?**
→ Run `v-demo-setup(with-dockerhub-pull).sh` + Stripe CLI

**Want to validate local code changes (full CI/CD pipeline)?**
→ Run `validate-cicd-locally.sh`

**Want to demo on VM to class?**
→ SSH to VM (https://console.cloud.google.com/compute/instances) → Run `vm-demo-launcher-(GCS).sh`

**GitHub Actions minutes exhausted?**
→ See `v-howto-push-fresh-image-to-dockerhub.txt` for manual push

---

## Google Cloud Storage CORS Configuration

**One-time setup for photo gallery feature** - Fixes browser CORS errors when loading thumbnails from GCS.

Run in Google Cloud Shell (https://console.cloud.google.com - click terminal icon):
```bash
echo '[{"origin": ["*"], "method": ["GET"], "responseHeader": ["Content-Type"], "maxAgeSeconds": 3600}]' > cors.json
gsutil cors set cors.json gs://eventhub-photos-prod
```

This is persistent - only needs to be done once per bucket.
