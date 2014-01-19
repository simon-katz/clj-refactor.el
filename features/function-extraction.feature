Feature: Function Extraction

  Background:
    Given I have a project "cljr" in "tmp"
    And I have a clojure-file "tmp/src/cljr/core.clj"
    And I open file "tmp/src/cljr/core.clj"
    And I clear the buffer

  Scenario: Extract defn
    When I insert:
    """
    (def d 3)

    (defn f [a]
      (let [b 1
            c 2]
        (+ 1 2 a b c d)))"
    """
    And I place the cursor before "(+ 1 2 a b c d)"
    And I press "C-! ed"
    And I type "my-fn"
    Then I should see:
    """
    (def d 3)

    (defn my-fn [a b c]
      (+ 1 2 a b c d))

    (defn f [a]
      (let [b 1
            c 2]
        (my-fn a b c d)))
    """