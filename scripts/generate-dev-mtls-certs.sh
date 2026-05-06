#!/usr/bin/env bash
set -euo pipefail

CERT_DIR="certs/dev"

mkdir -p "${CERT_DIR}"

echo "Generating local development CA..."

openssl genrsa -out "${CERT_DIR}/ca.key" 4096

openssl req \
  -x509 \
  -new \
  -nodes \
  -key "${CERT_DIR}/ca.key" \
  -sha256 \
  -days 3650 \
  -out "${CERT_DIR}/ca.crt" \
  -subj "/CN=hotelbooking-dev-ca"

echo "Generating inventory-service server certificate..."

openssl genrsa -out "${CERT_DIR}/inventory-service.key" 2048

openssl req \
  -new \
  -key "${CERT_DIR}/inventory-service.key" \
  -out "${CERT_DIR}/inventory-service.csr" \
  -subj "/CN=inventory-service"

cat > "${CERT_DIR}/inventory-service.ext" <<EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=DNS:localhost,DNS:inventory-service,IP:127.0.0.1
EOF

openssl x509 \
  -req \
  -in "${CERT_DIR}/inventory-service.csr" \
  -CA "${CERT_DIR}/ca.crt" \
  -CAkey "${CERT_DIR}/ca.key" \
  -CAcreateserial \
  -out "${CERT_DIR}/inventory-service.crt" \
  -days 825 \
  -sha256 \
  -extfile "${CERT_DIR}/inventory-service.ext"

echo "Generating booking-service client certificate..."

openssl genrsa -out "${CERT_DIR}/booking-service.key" 2048

openssl req \
  -new \
  -key "${CERT_DIR}/booking-service.key" \
  -out "${CERT_DIR}/booking-service.csr" \
  -subj "/CN=booking-service"

cat > "${CERT_DIR}/booking-service.ext" <<EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=clientAuth
subjectAltName=DNS:booking-service
EOF

openssl x509 \
  -req \
  -in "${CERT_DIR}/booking-service.csr" \
  -CA "${CERT_DIR}/ca.crt" \
  -CAkey "${CERT_DIR}/ca.key" \
  -CAcreateserial \
  -out "${CERT_DIR}/booking-service.crt" \
  -days 825 \
  -sha256 \
  -extfile "${CERT_DIR}/booking-service.ext"

rm -f "${CERT_DIR}"/*.csr "${CERT_DIR}"/*.ext "${CERT_DIR}"/*.srl

echo "Development mTLS certificates generated in ${CERT_DIR}"
echo
echo "Generated files:"
echo "  ${CERT_DIR}/ca.crt"
echo "  ${CERT_DIR}/inventory-service.crt"
echo "  ${CERT_DIR}/inventory-service.key"
echo "  ${CERT_DIR}/booking-service.crt"
echo "  ${CERT_DIR}/booking-service.key"