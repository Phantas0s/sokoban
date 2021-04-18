#!/bin/bash
# This is the fanciest way to deploy you'll ever see

clj -M:prod:play-cljc
rsync -arvz resources/public/* prod:/usr/share/nginx/html/mouseless/sokoban
