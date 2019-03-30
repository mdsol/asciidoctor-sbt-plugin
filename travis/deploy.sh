#!/usr/bin/env bash
set -ex

setup_git() {
  echo -e "machine github.com\n  login $GITHUB_AUTH_TOKEN" >> ~/.netrc
  git config --global user.email "${GIT_USER_EMAIL}"
  git config --global user.name "${GIT_USER_NAME}"
}

decrypt_private_key() {
  openssl aes-256-cbc -K $encrypted_aec9562d08d0_key -iv $encrypted_aec9562d08d0_iv -in travis/.gnupg/secring.asc.enc -out travis/.gnupg/secring.asc -d
}

decrypt_private_key
if [[ ${TRAVIS_BRANCH} == master ]] ; then
  setup_git
  git checkout master
  sbt ++$TRAVIS_SCALA_VERSION 'release with-defaults skip-tests'
elif [[ ${TRAVIS_BRANCH} == develop ]]; then
  sbt ++$TRAVIS_SCALA_VERSION publishSigned
fi
