#!/usr/bin/env bash
rm -fr _book
gitbook build .
cd _book
git init
git commit --allow-empty -m 'initial commit'
git checkout -b gh-pages
git add .
git commit -am 'updated docs'
git push --force https://github.com/RedHatWorkshops/amqv7-workshop.git gh-pages
