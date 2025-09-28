import { defineCollection } from 'astro:content';

import { glob } from 'astro/loaders';

const docs = defineCollection({
    loader: glob({ pattern: "**/*.md", base: "../docs" }),
});

export const collections = { docs };
