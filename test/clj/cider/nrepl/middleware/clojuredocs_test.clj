(ns cider.nrepl.middleware.clojuredocs-test
  (:require
   [cider.nrepl.test-session :as session]
   [orchard.clojuredocs :as docs]
   [clojure.string :as str]
   [clojure.test :refer :all]))

(def ^:private test-url "test/resources/cider/nrepl/clojuredocs/export.edn")

(use-fixtures :each session/session-fixture)

(deftest clojuredocs-refresh-cache-integration-test
  (testing "Invalid URL"
      (let [response (session/message {:op "clojuredocs-refresh-cache"
                                       :export-edn-url "/non-existing.edn"})]
        (is (contains? (:status response) "no-cache"))
        (is (not (str/blank? (:err response))))))

  (testing "Valid URL"
    (with-redefs [docs/test-to-access-url (constantly [false (java.io.IOException. "dummy")])]
      (docs/clean-cache!)
      (let [response (session/message {:op "clojuredocs-refresh-cache"
                                       :export-edn-url test-url})]
        (is (contains? (:status response) "no-cache"))))

    (with-redefs [docs/test-to-access-url (constantly [true])]
        (let [response (session/message {:op "clojuredocs-refresh-cache"
                                         :export-edn-url test-url})]
          (is (contains? (:status response) "ok"))))))

(deftest fixme-test
  (with-redefs [docs/accessible-url? (constantly false)]
    (docs/clean-cache!)
    (let [response (session/message {:op "clojuredocs-lookup"
                                     :ns "clojure.core"
                                     :sym "first"
                                     :export-edn-url test-url})]
      (is (contains? (:status response) "no-doc")))))

(deftest clojuredocs-lookup-integration-test
  (testing "Searching for non-existing documentation"
    (let [response (session/message {:op "clojuredocs-lookup"
                                     :ns "non-existing"
                                     :sym "non-existing"
                                     :export-edn-url test-url})]
      (is (contains? (:status response) "no-doc"))))

  (testing "Searching for existing documentation"
    (let [response (session/message {:op "clojuredocs-lookup"
                                     :ns "clojure.core"
                                     :sym "first"
                                     :export-edn-url test-url})
          doc (get response :clojuredocs {})]
      (is (contains? (:status response) "done"))
      (is (= "clojure.core" (:ns doc)))
      (is (= "first" (:name doc)))
      (is (every? #(contains? doc %) [:examples :see-alsos]))))

  (testing "Resolves syms in the supplied ns"
    (let [response (session/message {:op "clojuredocs-lookup"
                                     :ns "cider.nrepl.middleware.clojuredocs-test"
                                     :sym "map"
                                     :export-edn-url test-url})
          doc (get response :clojuredocs {})]
      (is (contains? (:status response) "done"))
      (is (= "clojure.core" (:ns doc)))
      (is (= "map" (:name doc)))
      (is (every? #(contains? doc %) [:examples :see-alsos])))))
