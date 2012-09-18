(ns kits.test.core
  (:use clojure.test
        kits.core
        conjure.core))

(deftest test-parse-number
  (are [expected str default] (= expected (parse-number str default))
    nil nil nil
    10 "10" nil
    nil "" nil
    24 "" 24
    10.0 "10.00" nil
    10.0 "10.00" 0
    10 10 0))

(deftest test-return-zero-if-negative
  (are [val expected] (= expected (return-zero-if-negative val))
    10 10
    -10 0
    "a" "Not Available"))

(deftest test-with-timeout
  (testing "macro version `with-timeout` executes block of code, throwing TimeoutException if timeout is hit"
    (let [caught-ex? (atom false)
          elapsed-ms (time-elapsed
        (try
          (with-timeout 500
            (while true
              (Thread/sleep 2)))
          (catch java.util.concurrent.TimeoutException _e_
            (reset! caught-ex? true))))]
      (is @caught-ex?)
      (is (> elapsed-ms 500))
      (is (< elapsed-ms 700))))

  (testing "fn version - upgrade a fn to a version that throws a TimeoutException if timeout is hit"
    (let [caught-ex? (atom false)
          timeout-version-of-fn (timeout-fn 500 (fn []
                                                  (while true
                                                    (Thread/sleep 2))))
          elapsed-ms (time-elapsed
        (try
          (timeout-version-of-fn)
          (catch java.util.concurrent.TimeoutException _e_
            (reset! caught-ex? true))))]
      (is @caught-ex?)
      (is (> elapsed-ms 500))
      (is (< elapsed-ms 700))))

  (testing "making sure it won't ALWAYS throw that TimeoutException"
    (with-timeout 10000
      (str "hi mom!"))))

(deftest =>-is-a-function-version-of-thread-first
  (is (= :99 (=> 99 str keyword))))

(deftest separates-seq-into-two-vecs---1st-matches-pred---2nd-fails-pred
  (is (= [["a" "b"] [1 2]] (segregate string? [1 "a" "b" 2])))
  (is (= [[] []] (segregate string? nil)))
  (is (= [[] []] (segregate string? []))))


;; periodic-fn
(deftest test-periodic-fn
  (let [msgs-logged (atom [])
        log-msg (fn [msg]
      (swap! msgs-logged conj msg))]
    (testing "creates a fn that only gets call every period times (2 in this example);
            has access to the 'call-count' as well, and the fn body is an implicit 'do'"
      (let [log-every-other (periodic-fn [msg] [call-count 2]
        (log-msg (format "%s-%s" msg call-count))
        (log-msg (format "%s-%s" msg call-count)))]

        (log-every-other "1 message")
        (is (= [] @msgs-logged) )

        (log-every-other "another message")
        (is (= ["another message-2" "another message-2"] @msgs-logged))

        (log-every-other "3rd message")
        (is (= ["another message-2" "another message-2"] @msgs-logged))

        (log-every-other "4th")
        (is (= ["another message-2" "another message-2" "4th-4" "4th-4"] @msgs-logged))))))

;;

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

(deftest test-boolean?
  (are [x bool?] (= bool? (boolean? x))
    false true
    true true
    nil  false
    "ad" false
    []   false
    {}   false))

(deftest test-fn->fn-thats-false-if-throws
  (are [f called-with result] (= ((fn->fn-thats-false-if-throws f) called-with) result)
    string? "s" true
    string? 99 false
    pos? "by itself, pos? would blow up given non-number" false))

(deftest test-non-neg-integer?
  (are [x result] (= (non-neg-integer? x) result)
    -4444444 false
    -333333  false
    -10000   false
    -999     false
    -77      false
    -5       false
    -3       false
    -2       false
    -1       false
    0        true
    1        true
    2        true
    3        true
    5        true
    77       true
    999      true
    10000    true
    333333   true
    4444444  true ))

(deftest test-url?
  (are [s result] (= (url? s) result)
    nil false
    ""  false
    "http://www.amazon.com" true))

(deftest test-ip-address-v4?
  (are [s result ] (= (ip-address-v4? s) result)
    nil false
    ""  false
    "192.168.10.255" true))

(deftest test-timestamp?
  (are [n result] (= (timestamp? n) result)
    nil      false
    -4444444 false
    -333333  false
    -10000   false
    -999     false
    -77      false
    -5       false
    -3       false
    -2       false
    -1       false
    0        true
    1        true
    2        true
    3        true
    5        true
    77       true
    999      true
    10000    true
    333333   true
    4444444  true
    0.0        false
    1.0        false
    2.0        false
    3.0        false
    5.0        false
    77.0       false
    999.0      false
    10000.0    false
    333333.0   false
    4444444.0  false
    9223372036854775807 true
    9223372036854775808 false))  ;; past the max long value

(deftest test-zip
  (are [lists result] (= (zip lists) result)
    nil []
    []  []
    [[:a 1 \x]]             [[:a] [1] [\x]]
    [[:a 1 \x] [:b 2 \y]]   [[:a :b] [1 2] [\x \y]]))

(deftest test-only
  (testing "when not 1 item"
    (is (thrown-with-msg? RuntimeException #"should have precisely one item, but had 0" (only [])))
    (is (= 1 (only [1])))
    (is (thrown-with-msg? RuntimeException #"should have precisely one item, but had at least 2" (only [1 2])))
    (is (thrown-with-msg? RuntimeException #"should have precisely one item, but had at least 2" (only (repeat 5))))))

(deftest test-nested-sort
  (are [input sorted] (= (nested-sort input) sorted)
    {} {}
    [] []
    nil nil
      #{} #{}))

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

(deftest test-ensure-sequential
  (are [result x] (= result (ensure-sequential x))
    [nil]     nil
    [{}]      {}
    [{:a 1}]  {:a 1}
    [1]       1
    [1 2 3]   [1 2 3]))

(deftest test-butlastv
  (are [result v] (and (= result (butlastv v))
                    (vector?  (butlastv v)))
    []      nil
    []      []
    []      [:a]
    [:a :b] [:a :b :c]))

(def throws-on-1st-or-2nd-call
  (let [cnt (atom 3)]
    (fn []
      (swap! cnt dec)
      (if (< @cnt 1)
        :foo
        (raise "BOOM!")))))

(deftest test-with-retries
  (is (= :foo (with-retries (throws-on-1st-or-2nd-call))))
  (is (thrown? Exception (with-retries (raise Exception "BLAMMO!")))))

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

(deftest test-transform-fake-json-params->map
  (are [expected params] (= expected (transform-fakejson-params->map params))
    {} {}
    {"a" "1"} {"a" "1"}
    {"a" {"b" {"c" {"d" "e"}}}} {"a[b][c][d]" "e"}
    {"a-1" {"b_2" {"c_d-3" "e"}}} {"a-1[b_2][c_d-3]" "e"}))

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

(defmacro appropriate-stubbing []
  (if (= 2 (:minor *clojure-version*))
    'binding
    'stubbing)

(deftest test-single-destructuring-arg->form+name
  ((appropriate-stubbing)) [gensym 'unique-3]
    (are [original form name] (let [[frm nm] (single-destructuring-arg->form+name original)]
                                (and (= frm form)
                                  (= nm name)))
      'a                     'a                          'a
      '[a b]                 '[a b :as unique-3]         'unique-3
      '[a b & c :as all]     '[a b & c :as all]          'all
      '{:keys [a b]}         '{:keys [a b] :as unique-3} 'unique-3
      '{:keys [a b] :as all} '{:keys [a b] :as all}      'all
      ;; pathological cases
      '[a]                   '[a :as unique-3]           'unique-3
      '[a :as b]             '[a :as b]                  'b)))