#!/bin/sh
java -cp classes ir.TokenTest -f token_test.txt -p patterns.txt -rp -cf > tokenized_result.txt && diff tokenized_result.txt token_test_tokenized_ok.txt --strip-trailing-cr