{:room   [{:id     "w00t"
           :light  []
           :name   "w00t"
           :scene  ["fix" "hello"]
           :sensor []}
          {:id     "f00"
           :light  []
           :name   "f00"
           :scene  ["foo"]
           :sensor []}]
 :light  []
 :scene  [{:flows       {}
           :id          "hello"
           :mode        "none"
           :name        "hello"
           :nodes       {}
           :on?         false
           :room-id     "w00t"
           :translation {:x 0
                         :y 0}}
          {:flows       {}
           :id          "foo"
           :mode        "none"
           :name        "foo"
           :nodes       {}
           :on?         false
           :room-id     "f00"
           :translation {:x 0
                         :y 0}}
          {:flows       {}
           :id          "fix"
           :mode        "none"
           :name        "fix"
           :nodes       {:b04e16c6-b856-44e4-bb14-7d664eb4981d {:id       "b04e16c6-b856-44e4-bb14-7d664eb4981d"
                                                                :ilk      "mixer"
                                                                :item-id  "b73ec2be-89d0-4cea-9aa5-8c1a9a86ff07"
                                                                :links    {:b04e16c6-b856-44e4-bb14-7d664eb4981d-0   {:direction "in"
                                                                                                                      :id        "b04e16c6-b856-44e4-bb14-7d664eb4981d-0"
                                                                                                                      :ilk       "value"
                                                                                                                      :index     0
                                                                                                                      :name      "first"}
                                                                           :b04e16c6-b856-44e4-bb14-7d664eb4981d-1   {:direction "in"
                                                                                                                      :id        "b04e16c6-b856-44e4-bb14-7d664eb4981d-1"
                                                                                                                      :ilk       "value"
                                                                                                                      :index     1
                                                                                                                      :name      "second"}
                                                                           :out-b04e16c6-b856-44e4-bb14-7d664eb4981d {:direction "out"
                                                                                                                      :id        "out-b04e16c6-b856-44e4-bb14-7d664eb4981d"
                                                                                                                      :ilk       "value"
                                                                                                                      :index     2
                                                                                                                      :name      "out"}}
                                                                :position {:x 201
                                                                           :y 98}}}
           :on?         false
           :room-id     "w00t"
           :translation {:x 0
                         :y 0}}]
 :color  []
 :signal ()
 :mixer  [{:id     "b73ec2be-89d0-4cea-9aa5-8c1a9a86ff07"
           :mix-fn "add"}]
 :config {:signal {:sparkline/timespan-seconds 15}}}