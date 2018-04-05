(ns perfect-weather.analysis.ui.app
  (:require
    [re-frame.core :refer [subscribe]]
    [perfect-weather.data.rate :as rate]
    [perfect-weather.data.months :refer [months-abbr]]
    [perfect-weather.data.summary :as summary]
    [perfect-weather.data.filters :as filters]))

(defn hourly-view [data {:keys [bars? clip?]}]
  (let [h 2
        w 2
        clip (if clip?
               (fn [coll]
                 (->> coll
                      (drop rate/hour-start)
                      (take rate/hour-count)))
               identity)]
    [:div {:style {:display "flex"
                   :position "relative"}}
     (when bars?
       [:div
        [:div {:style {:position "absolute"
                       :top (str (+ 2 (* h rate/hour-start)) "px")
                       :margin-top "1em"
                       :z-index 10
                       :width "100%"
                       :height "1px"
                       :background "white"}}]
        [:div {:style {:position "absolute"
                       :top (str (+ 2 (* h rate/hour-end)) "px")
                       :margin-top "1em"
                       :z-index 10
                       :width "100%"
                       :height "1px"
                       :background "white"}}]])
     (->> data
          (partition 30)
          (map-indexed (fn [i month]
                         ^{:key i}
                         [:div.month 
                          [:div (months-abbr i)]
                          [:div {:style {:display "flex"}}
                           (for [day month]
                             ^{:key (-> day first :id)}
                             [:div.day {:style {:background "black"}}
                              (for [row (clip day)]
                                ^{:key (row :id)}
                                [:div.hour {:style {:width (str w "px")
                                                    :height (str h "px") 
                                                    :background (row :value)}}])])]])))]))
(defn bar-view [data]
  [:div {:style {:display "flex"
                 :position "relative"}}
   (->> data
        (partition 30)
        (map-indexed (fn [i month]
                       ^{:key i}
                       [:div.month 
                        [:div {:style {:display "flex"}}
                         (->> month
                              (map-indexed (fn [i day]
                                             ^{:key i}
                                             [:div.day 
                                              [:div.hour {:style {:width "2px"
                                                                  :height "5px"
                                                                  :background (if day
                                                                                "#4cafef"
                                                                                "white")}}]])))]])))])

(defn app-view []
  [:div
   (doall
     (for [place @(subscribe [:data])]
       ^{:key (place :city)}
       [:div
        [:h2 (place :city)]

        [:table

         #_[:tbody
            [:tr
             [:td "Temperature"]
             [:td
              [hourly-view 
               (->> (place :data)
                    (map (fn [day]
                           (map (fn [row]
                                  {:id (row :epoch)
                                   :value (str "hsl(204,84%," (/ (* 100 (row :temperature))
                                                                 40) "%)")})
                                day))))
               {:bars? true
                :clip? false}]]]
            [:tr
             [:td "Humidity"]
             [:td
              [hourly-view 
               (->> (place :data)
                    (map (fn [day]
                           (map (fn [row]
                                  {:id (row :epoch)
                                   :value (str "hsl(204,84%," (* 100 (row :humidity)) "%)")})
                                day))))
               {:bars? true
                :clip? false}]]]
            [:tr
            [:tr
             [:td "Precipitation"]
             [:td
              [hourly-view 
               (->> (place :data)
                    (map (fn [day]
                           (map (fn [row]
                                  {:id (row :epoch)
                                   :value (if (row :precipitation?)
                                            "#4cafef"
                                            "black")})
                                day))))
               {:bars? true
                :clip? false}]]]]

         [:tbody
          [:tr
           [:td "Nice Hours"]
           [:td
            [hourly-view 
             (->> (place :data)
                  (map (fn [day]
                         (map (fn [row]
                                {:id (row :epoch)
                                 :value (case (rate/issue row)
                                          ;:hot "#880000"
                                          ;:cold "#000088"
                                          ;:humid "#888800"
                                          ;:dry "#880088"
                                          ;:rain "red"
                                          :nice "#4cafef"
                                          ;:perfect "#70fffb"
                                          "black")})
                              day))))
             {:bars? true
              :clip? false}]]]]

         [:tbody
          (for [[title f] [["Hot" rate/hot?]
                           ["Cold" rate/cold?]
                           ["Humid" rate/humid?]
                           ["Dry" rate/dry?]]]
            ^{:key title}
            [:tr
             [:td title]
             [:td 
              [bar-view (->> (place :data)
                             (map (fn [hours]
                                    (rate/day-result? f false hours))))]
              [bar-view (->> (place :data)
                             (map (fn [hours]
                                    (rate/day-result? f false hours)))
                             (filters/combined-filter))]]])]
         [:tbody
          [:tr
           [:td "Nice Days"]
           [:td [bar-view (->> (place :data)
                               (map (fn [hours]
                                      (rate/day-result? rate/nice? true hours))))]]]
          [:tr
           [:td "Nice Days (filtered)"]
           [:td [bar-view (->> (place :data)
                               (map (fn [hours]
                                      (rate/day-result? rate/nice? true hours)))
                               (filters/combined-filter))]]]]
         #_[:tbody
            [:tr
             [:td "Summary"]
             [:td (summary/text (->> (place :data)
                                     (map (fn [hours]
                                            (rate/day-result? rate/nice? true hours)))
                                     (filters/combined-filter)))]]
            [:tr
             [:td "Nice Days"]
             [:td (summary/days-count (->> (place :data)
                                           (map (fn [hours]
                                                  (rate/day-result? rate/nice? true hours)))
                                           (filters/combined-filter)))]]]]]))]) 

