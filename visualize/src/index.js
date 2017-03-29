'use strict';

// Create the Google Map…
const map = new google.maps.Map(d3.select("#map").node(), {
    zoom: 12,
    center: new google.maps.LatLng(40.7, -73.975),
    mapTypeId: google.maps.MapTypeId.TERRAIN
});


const overlay = new google.maps.OverlayView();
const select = document.getElementById("timeselect");
const topLeftX = 40.9;
const topLeftY = -74.25;
const bottomRightX = 40.5;
const bottomRightY = -73.7;

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

        const projection = this.getProjection();
        const gridSize = 200;
        const deltaLat = (bottomRightX - topLeftX) / gridSize;
        const deltaLon = (bottomRightY - topLeftY) / gridSize;
		
        let grid = [];
        for (let i = 0; i < gridSize; i++) {
            for (let j = 0; j < gridSize; j++) {
                grid.push({lat: topLeftX + deltaLat * i, lon: topLeftY + deltaLon * j, value: value[j + i * gridSize]});
            }
        }
		
		//var hotspots = grid;
		var hotspots = getTopN(grid, "value", 500);

        layer.selectAll(".border")
            .data([{lat: topLeftX, lon: topLeftY}])
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
            .data(hotspots)

        tile.enter()
            .append("svg")
            .each(transformTile)
            .attr("class", "grid");

        tile.append("rect")
            .attr(
                {
                    "class": "tile",
                    "fill": "none",
                    "shape-rendering": "crispEdges",
                    "stroke": "black",
                    "stroke-width": "1px"
                })
            .each(transformTile);
			
		function getTopN(arr, prop, n) {
			var clone = arr.slice(0);
			clone.sort(function(x, y) {
				if (x[prop] == y[prop]) return 0;
				else if (parseInt(x[prop]) < parseInt(y[prop])) return 1;
				else return -1;
			});
			return clone.slice(0, n || 1);
		}

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
            let color = d3.scale.linear().domain([-1, 1,5, 40]).range(["blue", "green", "yellow", "red"]);
            let topLeft = new google.maps.LatLng(d.lat, d.lon);
            let bottomRight = new google.maps.LatLng(d.lat + deltaLat, d.lon + deltaLon);

            topLeft = projection.fromLatLngToDivPixel(topLeft);
            bottomRight = projection.fromLatLngToDivPixel(bottomRight);

            

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