Feature: Function Extraction

  Background:
    Given I have a project "cljr" in "tmp"
    And I have a clojure-file "tmp/src/cljr/core.clj"
    And I open file "tmp/src/cljr/core.clj"
    And I clear the buffer

  Scenario: Extract defn
    When I insert:
    """
    (defn f [a]
      (let [b 1
            c 2]
        (+ 1 2 a b c)))"
    """
    And I place the cursor before "(+ 1 2 a b c)"
    And I press "C-! ed"
    And I type "my-fn"
    Then I should see:
    """
    (defn my-fn [a b c]
      (+ 1 2 a b c))

    (defn f [a]
      (let [b 1
            c 2]
        (my-fn a b c)))
    """