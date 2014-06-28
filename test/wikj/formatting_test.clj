(ns wikj.formatting-test
  (:use clojure.test
        wikj.formatting))

(deftest test-wiki->html

  (testing "links"
    (are [expected input] (= expected (wiki->html-links input))

         "<a href='a+link'>a link</a>"
         "[[a link]]"

         "<a href='link'>a link</a>"
         "[[link][a link]]"))

  (testing "new lines"
    (are [expected input] (= expected (wiki->html-newlines input))

         "first<br>second"
         "first\nsecond"

         "first<br>second"
         "first\n\rsecond"

         "first<p>second"
         "first\n\nsecond"

         "first<p>second"
         "first\n\r\n\rsecond"))

  (testing "all together"
    (are [expected input] (= expected (wiki->html input))

         "Just a <a href='a+link'>link</a>.<br>On a &lt;new&gt; line.<p>And a paragraph."
         "Just a [[a link][link]].\nOn a <new> line.\n\n\rAnd a paragraph.")))
