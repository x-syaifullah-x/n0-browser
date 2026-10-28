#!/bin/sh

openssl s_client -connect n0render.com:443 -servername n0render.com -showcerts </dev/null 2>/dev/null \
  | awk '/BEGIN CERT/{n++} n==3' | sed '/END CERT/q' > ssl_com_root_2022.pem

openssl x509 -in ssl_com_root_2022.pem -noout -subject -fingerprint -sha256