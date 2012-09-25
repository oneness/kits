(ns kits.test.map
  (:use clojure.test
        kits.map))


(deftest test-update-in-if-present
  (are [result m] (= result (update-in-if-present m [:a :b]  (constantly 99)))

    ;; result       inputted map
    {:a {:b 99}}    {:a {:b 1}}
    {:a 2}          {:a 2}
    {:a {:b 99}}  {:a {:b nil}}
    {:a {:b 99}}  {:a {:b false}}
    nil             nil
    {}              {}))

(deftest test-paths
  (are [m result] (= (paths m) result)
    nil            nil
    {:a 1}         '([:a])
    {:a 1
     :b 2}         '([:a] [:b])
    {:a 1
     :b {:c 2
         :d 3}}    '([:a] [:b :c] [:b :d])))

(deftest test-subpaths
  (are [path sps] (= (subpaths path) sps)
    nil        []
    [:a]       [[:a]]
    [:a :b :c] [[:a] [:a :b] [:a :b :c]]))

(deftest test-subpath?
  (are [root-path path result] (= (subpath? root-path path) result)
    nil nil false
    [:a] [:a] true
    [:a] [:a :b] true
    [:a] [:a :b :c] true
    [:a :b] [:c] false
    [:a :b] [:a] false
    [:a :b] [:a :b] true
    [:a :b] [:a :e :c] false))

(deftest test-select-paths
  (are [m paths result] (= (apply select-paths m paths) result)
    nil                  [[:a :b]] {:a {:b nil}}
    {}                   [[:a :b]] {:a {:b nil}}
    {:a {:b 1}}          [[:a :b]] {:a {:b 1}}

    {:a {:b 1 :c 2 :d 3}
     :x {:y 4 :z 5}
     :n {:m 77}}     [[:a :b] [:a :c] [:x]] {:a {:b 1 :c 2} :x {:y 4 :z 5}}
    ))

(deftest test-contains-path?
  (is (false? (contains-path? nil [:a])))
  (is (false? (contains-path? nil nil)))
  (is (false? (contains-path? nil [])))
  (is (false? (contains-path? {} [:a])))
  (is (false? (contains-path? {} nil)))
  (is (false? (contains-path? {} [])))
  (is (true? (contains-path? {:a 1} [:a])))
  (is (true? (contains-path? {:a {:b 1}} [:a :b]))))

(deftest test-invert-map
  (are [in-map out-map] (invert-map in-map)
    nil           nil
    {}            {}
    {:a 1}        {1 :a}
    {:a {:b 1}}   {{:b 1} :a}

    ;; if the vals aren't unique then funky things will result - beware
    {:a :b :c :b} {:b :c}))

(deftest test-dissoc-in
  (are [m paths result] (= result (apply dissoc-in m paths))
    {} [[:a :b]] {}
    nil [[:a :b]] nil
    {:a {:b 1 :c 2 :d 3}} [[:a :b] [:a :c]] {:a {:d 3}}))

(deftest test-nested-dissoc
  (are [result x] (= result (nested-dissoc x :a))
    nil nil
    1 1
    {} {}
      #{} #{}

    [1 1] [1 1]
    [{:b 2}] [{:a 1 :b 2}]
      #{{:b 2}} #{{:a 1 :b 2}}

    [{:b [{:b 3}]}] [{:a 1 :b [{:a 1 :b 3}]}]
    ))

(deftest test-filter-map
  (are [m pred result] (= result (filter-map pred m))
    {}
    (constantly false)
    {}

    nil
    (constantly true)
    {}

    {:a 1 :b 2}
    (fn [k v] (and (= k :a) (= v 1)))
    {:a 1}))

(deftest test-filter-by-key
  (are [m pred result] (= result (filter-by-key pred m))
    {}
    (constantly false)
    {}

    nil
    (constantly true)
    {}

    {:a 1 :b 2}
    (fn [k] (= k :a))
    {:a 1}))

(deftest test-filter-by-val
  (are [m pred result] (= result (filter-by-val pred m))
    {}
    (constantly false)
    {}

    nil
    (constantly true)
    {}

    {:a 1 :b 2}
    (fn [v] (= v 1))
    {:a 1}))

(deftest test-map-over-map
  (are [m f result] (= result (map-over-map f m))
    {}
    (constantly false)
    {}

    nil
    (constantly true)
    {}

    {:a 1 :b 2}
    (fn [k v] [(name k) (inc v)])
    {"a" 2 "b" 3}))

(deftest test-map-over-map
  (is (= {:a 2 :b 3 :c 4 :d 5}) (map-over-map (fn [k v] [k (inc v)]) {:a 1 :b 2 :c 3 :d 4})))

(deftest test-map-values
  (is (= {:b 8, :a 7} (map-values #(+ 5 %) {:a 2 :b 3})))
  (is (= {} (map-values #(+ 5 %) {})))
  (is (= {} (map-values #(+ 5 %) nil)))
)

(deftest test-keyword-munging
  (is (= "a_foo_1" (keyword->underscored-string :a-foo-1)))
  (is (= :a_foo_1 (keyword->underscored-keyword :a-foo-1)))
  (is (= "a-foo-1" (keyword->hyphenated-string :a_foo_1)))
  (is (= :a-foo-1 (keyword->hyphenated-keyword :a_foo_1)))
  (is (= :a-foo-1 (keywords->hyphenated-keywords :a_foo_1)))
  (is (= :a-foo-1 (keywords->hyphenated-keywords :a-foo-1)))
  )

(deftest test-keywords->underscored-keywords
  (is (= {:a_1 1
          :b_1 1
          "c-1" 1}
        (keywords->underscored-keywords {:a-1 1
                                         :b-1 1
                                         "c-1" 1}))))

(deftest test-keywords->underscored-strings
  (is (= {"a_1" 1
          "b_1"  1
          "c-1" 1}
        (keywords->underscored-strings {:a-1 1
                                        :b-1 1
                                        "c-1" 1}))))

(deftest test-keys-to-keywords
  (testing "when :underscore-to-hyphens? is true (true by default)"
    (are [expected m] (= expected (keys-to-keywords m :underscore-to-hyphens? true))
      {} {}
      {:a "1"} {"a" "1"}
      {:a {:b {:c {:d "e"}}}} {"a" {"b" {"c" {"d" "e"}}}}
      {:a-1 {:b-2 {:c-d-3 "e"}}} {"a-1" {"b_2" {"c_d-3" "e"}}}))
  (testing "when :underscore-to-hyphens? is true (true by default)"
    (are [expected m] (= expected (keys-to-keywords m :underscore-to-hyphens? false))
      {} {}
      {:a "1"} {"a" "1"}
      {:a {:b {:c {:d "e"}}}} {"a" {"b" {"c" {"d" "e"}}}}
      {:a-1 {:b_2 {:c_d-3 "e"}}} {"a-1" {"b_2" {"c_d-3" "e"}}})))