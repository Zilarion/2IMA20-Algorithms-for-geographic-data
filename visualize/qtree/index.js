'use strict';
// Top left: [40,9, -74.25]
// Bottom right: [40.5, -73.7]


let quadtree;

window.initMap = function() {
    d3.json("qtree.json", function (error, data) {
        if (error) throw error;
        var google = window.google;
        function SVGOverlay (map) {
            this.map = map;
            this.svg = null;
            this.quadtree = null;

            this.onPan = this.onPan.bind(this);

            this.setMap(map);
        }

        SVGOverlay.prototype = new google.maps.OverlayView();

        SVGOverlay.prototype.onAdd = function () {
            this.svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
            this.svg.style.position = 'absolute';
            this.svg.style.top = 0;
            this.svg.style.left = 0;
            this.svg.style.width = '100%';
            this.svg.style.height = '100%';
            this.svg.style.pointerEvents = 'none';

            const bounds = this.map.getBounds();

            const proj = this.getProjection();
            this.quadtree = data;

            this.qt = generate_quadtree_map(this.map, this.svg, this.points, proj);

            this.onPan();
            document.body.appendChild(this.svg);
            this.map.addListener('center_changed', this.onPan);
        };

        SVGOverlay.prototype.onPan = function () {
            let proj = this.getProjection();
			this.qt = generate_quadtree_map(this.map, this.svg, this.points, proj);
            redraw_qt(this.svg, this.qt, proj, this.points);
        };

        SVGOverlay.prototype.onRemove = function () {
            this.map.removeListener('center_changed', this.onPan);
            this.svg.parentNode.removeChild(this.svg);
            this.svg = null;
        };

        SVGOverlay.prototype.draw = function () {
            let proj = this.getProjection();
            this.qt = generate_quadtree_map(this.map, this.svg, this.points, proj);
        };

        // Create the Google Map…
        const map = new google.maps.Map(d3.select("#map").node(), {
            zoom: 11,
            center: new google.maps.LatLng(40.7, -73.975),
            mapTypeId: google.maps.MapTypeId.TERRAIN
        });

        let overlay = new SVGOverlay(map);
    });
};

function transformXY(proj, x, y) {
    return proj.fromLatLngToContainerPixel(new google.maps.LatLng(x, y));
}

function generate_quadtree_map(map, svg, pt_arr, proj) {
    quadtree = d3.geom.quadtree(pt_arr);
	
	const bounds = map.getBounds();
	const quads = [];
	nodes(quadtree).forEach(function(n) {
		if (bounds.getSouthWest().lng() <= n.y &&
			bounds.getNorthEast().lng() >= n.y &&
			bounds.getSouthWest().lat() <= n.x &&
			bounds.getNorthEast().lat() >= n.x &&
			n.depth >= 0.6*quadtree.max_depth) {
				quads.push(n);
			}
	});

    const this_quadtree = quads;

    d3.select(svg)
        .selectAll("*").remove();

    let node = d3.select(svg)
        .selectAll(".node")
        .data(quadtree_nodes);

    let col = d3.scale.linear()
        .domain([0, 40])
        .range(['green', 'red']);

    node.enter().append("rect")
        //
        .attr("x", function (d) {
            return transformXY(proj, d.x1, d.y1).x;
        })
        .attr("y", function (d) {
            return transformXY(proj, d.x2, d.y2).y;
        })
        .attr("width", function (d) {
            let p1 = transformXY(proj, d.x1, d.y1);
            let p2 = transformXY(proj, d.x2, d.y2);

            return p2.x - p1.x;
        })
        .attr("height", function (d) {
            let p1 = transformXY(proj, d.x1, d.y1);
            let p2 = transformXY(proj, d.x2, d.y2);
            return p1.y - p2.y;
        })
        .style("position", "absolute")
        .attr('fill-opacity', function (d) {
            // return 0.3;
			return (d.depth / quadtree.max_depth);
        })
        .style("fill", (d) => col(d.depth))
        .attr("class", "node");
    node.exit().remove();
}

function redraw_qt(svg, quadtree_nodes, proj) {
    let node = d3.select(svg)
        .selectAll(".node")
        .data(quadtree_nodes)
        .attr("x", function (d) {
            return transformXY(proj, d.x1, d.y1).x;
        })
        .attr("y", function (d) {
            return transformXY(proj, d.x2, d.y2).y;
        });

    // d3.select(svg)
    //     .selectAll(".point")
    //     .data(points)
    //     .attr("cx", (d) => transformXY(proj, d.x, d.y).x)
    //     .attr("cy", (d) => transformXY(proj, d.x, d.y).y)
}