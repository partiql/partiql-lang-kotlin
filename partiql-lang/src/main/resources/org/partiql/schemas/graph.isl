$ion_schema_2_0

/** This ISL schema is a specification of an Ion-based file format for defining graphs externally,
    in order to be brought into PartiQL data model for querying with the PartQL implementation of GPML.
    This is useful for general reasons of accessing external data, but is particularly necessary
    for graphs, since PartiQL does not yet define concrete syntax for them.

    The design of the schema closely follows the graph data model description in
    PartiQL [RFC-0025](https://github.com/partiql/partiql-docs/blob/main/RFCs/0025-graph-data-model.md),
    with the exception that the data model in this format is not recursive: the payloads at nodes and edges
    are general Ion.  That is, it is not specified how a fragment of a payload can be itself recognized as a graph.

    A file contains a graph if it contains a single Ion struct of ISL type Graph, as defined here.

    Annotations specified in this schema (graph, node, edge, dir, undir) are for mnemonic purposes only --
    graph authors are not required to use them and processors should not attribute semantic meaning to their
    presence or absence.
*/

type::{
    name: Graph,
    annotations: closed::[graph],
    type: struct,
    fields: closed::{
        nodes: { type: list, element: Node, occurs: required },
        edges: { type: list, element: Edge, occurs: required }
    }
}

type::{
    name: Node,
    annotations: closed::[node],
    type: struct,
    fields: closed::{
        id: { type: NodeId, occurs: required },
        labels: { type: list, element: string, occurs: optional },
        payload: { type: NodePayload, occurs: optional }
    }
}

type::{
    name: Edge,
    annotations: closed::[edge],
    type: struct,
    fields: closed::{
        id: { type: EdgeId, occurs: required },
        labels: { type: list, element: string},
        ends: { one_of: [DirectedPair, UndirectedPair], occurs: required },
        payload: { type: EdgePayload, occurs: optional }
    }
}

type::{
    name: DirectedPair,
    type: list,
    ordered_elements: [ NodeId, NodeId ],
    annotations: closed::[dir]
}

type::{
    name: UndirectedPair,
    type: sexp,
    ordered_elements: [ NodeId, NodeId ],
    annotations: closed::[undir]
}

type::{ name: NodeId, type: symbol, annotations: closed::[node] }
type::{ name: EdgeId, type: symbol, annotations: closed::[edge] }

type::{ name: NodePayload, type: $any }
type::{ name: EdgePayload, type: $any }
