#!/bin/bash
# Run this ONCE before `docker compose up`, from the repo root:
#   chmod +x localstack/generate-keys.sh && ./localstack/generate-keys.sh
#
# Generates the RSA key pair used for CloudFront signed URLs.
# - private_key.pem -> given to your backend, used by
#   CloudFrontUtilities.getSignedUrlWithCannedPolicy(...)
# - public_key.pem   -> uploaded into CloudFront (via the CFN template)
#                        as an AWS::CloudFront::PublicKey

set -e

KEY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/keys"
mkdir -p "$KEY_DIR"

if [ -f "$KEY_DIR/private_key.pem" ]; then
  echo "Key pair already exists at $KEY_DIR, skipping generation."
  exit 0
fi

echo "Generating RSA key pair for CloudFront signed URLs..."

# CloudFront requires 2048-bit RSA keys in PEM (SSH-2 / PKCS#1 or SubjectPublicKeyInfo) format.
openssl genrsa -out "$KEY_DIR/private_key.pem" 2048
openssl rsa -pubout -in "$KEY_DIR/private_key.pem" -out "$KEY_DIR/public_key.pem"

chmod 600 "$KEY_DIR/private_key.pem"

echo "Done. Keys written to $KEY_DIR"
echo "  private_key.pem -> keep this secret, load it into your backend"
echo "  public_key.pem  -> consumed by the CloudFormation template"