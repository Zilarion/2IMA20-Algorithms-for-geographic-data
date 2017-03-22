'use strict';

// Create the Google Map…
const map = new google.maps.Map(d3.select("#map").node(), {
    zoom: 12,
    center: new google.maps.LatLng(40.7, -73.975),
    mapTypeId: google.maps.MapTypeId.TERRAIN
});


const overlay = new google.maps.OverlayView();
const select = document.getElementById("timeselect");

// Top left: [40,9, -74.25]
// Bottom right: [40.5, -73.7]
d3.json("results.json", function(error, data) {
    if (error) throw error;

    for (let key in data) {
        let option = document.createElement("option");
        option.text = key;
        select.add(option);
    }

    overlay.draw = function () {
        const value = data[select.value];
        d3.selectAll(".gridOverlay").remove();
        const layer = d3.select(this.getPanes().overlayLayer).append("div")
            .attr("class", "gridOverlay");
        console.log(layer)
        const projection = this.getProjection();
        const gridSize = 50;
        const deltaLat = (40.5 - 40.9) / gridSize;
        const deltaLon = (-73.7 - -74.25) / gridSize;

        let grid = [];
        for (let i = 0; i < gridSize; i++) {
            for (let j = 0; j < gridSize; j++) {
                grid.push({lat: 40.9 + deltaLat * i, lon: -74.25 + deltaLon * j, value: value[j + i * gridSize]});
            }
        }

        layer.selectAll(".border")
            .data([{lat: 40.9, lon: -74.25}])
            .each(transformBorder)
            .enter()
            .append("rect")
            .attr({
                "class": "border",
                "fill": "none",
                "shape-rendering": "crispEdges",
                "stroke": "red",
                "stroke-width": "4px"
            }).each(transformBorder);

        const tile = layer.selectAll("svg")
            .data(grid)

        tile.enter()
            .append("svg")
            .each(transformTile)
            .attr("class", "grid");

        tile.append("rect")
            .attr(
                {
                    "class": "tile",
                    "fill": "none"
                })
            .each(transformTile);

        function transformBorder(d) {
            let topLeft = new google.maps.LatLng(d.lat, d.lon);
            let bottomRight = new google.maps.LatLng(d.lat + deltaLat * gridSize, d.lon + deltaLon * gridSize);

            topLeft = projection.fromLatLngToDivPixel(topLeft);
            bottomRight = projection.fromLatLngToDivPixel(bottomRight);

            return d3.select(this)
                .style("left", (topLeft.x) + "px")
                .style("top", (topLeft.y) + "px")
                .style("width", (topLeft.x - bottomRight.x) + "px")
                .style("height", (topLeft.y - bottomRight.y) + "px")
        }

        function transformTile(d) {
            let color = d3.scale.linear().domain([-3,0, 8]).range(["green", "grey", "red"]);
            let topLeft = new google.maps.LatLng(d.lat, d.lon);
            let bottomRight = new google.maps.LatLng(d.lat + deltaLat, d.lon + deltaLon);

            topLeft = projection.fromLatLngToDivPixel(topLeft);
            bottomRight = projection.fromLatLngToDivPixel(bottomRight);

            // const text = d3.select(this)
            //     .append("text")
            //     .text(d.value.toFixed(2))
            //     .style("fill", "white")
            //     .style("opacity", 0.6)
            //     .style("font-size", 7)
            //     .attr({
            //         "x": "50%",
            //         "y": "50%",
            //         "alignment-baseline": "middle",
            //         "text-anchor": "middle"
            //     });

            return d3.select(this)
                .style("left", (topLeft.x) + "px")
                .style("top", (topLeft.y) + "px")
                .style("width", (bottomRight.x - topLeft.x) + "px")
                .style("height", (bottomRight.y - topLeft.y) + "px")
                .style("opacity", 0.6)
                .style("background-color", color(d.value));
        }
    };
    overlay.setMap(map);
});


function timeChange() {
    overlay.draw()
}