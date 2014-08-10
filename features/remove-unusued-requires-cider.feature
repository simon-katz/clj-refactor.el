@cider
Feature: remove unused require using cider and nrepl middleware

  Background:
    Given I have a project "cljr" in "tmp"
    And I use refactor-nrepl
    And I run cider-jack-in
    And I have a clojure-file "tmp/src/cljr/core.clj"
    And I open file "tmp/src/cljr/core.clj"
    And I press "M-<"
    And I switch auto-sort off

  Scenario: cider keeps it if referenced
    Then I should have a cider session with refactor-nrepl
    When I insert:
    """
    (ns cljr.core
      (:require [clojure.string :refer [trim]]
                [clojure.set :refer [difference]]
                [clj-time.core]))

    (defn use-time []
      (clj-time.core/now)
      ;;(trim "  foobar ")
      (difference #{:a :b} #{:a :c}))
    """
    And I place the cursor before "now"
    And I press "C-! rr"
    Then I should see message "refactor-nrepl is used"
    Then I should see:
    """
    (ns cljr.core
      (:require [clojure.set :refer [difference]]
                [clj-time.core]))

    (defn use-time []
      (clj-time.core/now)
      ;;(trim "  foobar ")
      (difference #{:a :b} #{:a :c}))
    """
